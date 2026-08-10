package net.conczin.mca.livingworld.relationship;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.conczin.mca.livingworld.persistence.GsonJsonStoreCodec;
import net.conczin.mca.livingworld.persistence.JsonStoreRecovery;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** World-local persistent directed NPC-to-NPC social graph. */
public final class NpcSocialGraphStore {
    static final int MAX_OUTGOING_EDGES_PER_NPC = 64;

    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonStoreRecovery.Codec<GraphFile> CODEC =
            new GsonJsonStoreCodec<>(GSON, GraphFile.class);
    private static final ConcurrentMap<Path, NpcSocialGraphStore> STORES = new ConcurrentHashMap<>();

    private final Path file;
    private final Set<UUID> blockedCausalSources = new HashSet<>();
    private GraphFile data;

    public static NpcSocialGraphStore forWorld(Path worldRoot) {
        Path file = worldRoot.toAbsolutePath().normalize()
                .resolve("livingworld")
                .resolve("npc-social-graph.json");
        return STORES.computeIfAbsent(file, NpcSocialGraphStore::new);
    }

    NpcSocialGraphStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
        this.data = load();
    }

    public synchronized NpcSocialState get(UUID sourceNpcId, UUID targetNpcId) {
        if (!validPair(sourceNpcId, targetNpcId)) return NpcSocialState.NEUTRAL;
        NpcSocialState state = data.edges.get(key(sourceNpcId, targetNpcId));
        return state == null ? NpcSocialState.NEUTRAL : state;
    }

    public synchronized Optional<NpcSocialMutationCursor> latestCausalMutation(UUID sourceNpcId) {
        if (sourceNpcId == null || blockedCausalSources.contains(sourceNpcId)) return Optional.empty();
        return Optional.ofNullable(data.causalFrontiers.get(sourceNpcId.toString()));
    }

    public synchronized NpcSocialGraphMutation applyDelta(
            UUID sourceNpcId,
            UUID targetNpcId,
            NpcSocialDelta proposed,
            int maxDeltaPerMutation
    ) {
        if (!validPair(sourceNpcId, targetNpcId)) {
            return new NpcSocialGraphMutation(
                    NpcSocialGraphMutation.Status.INVALID_PAIR,
                    sourceNpcId,
                    targetNpcId,
                    NpcSocialState.NEUTRAL,
                    NpcSocialState.NEUTRAL
            );
        }

        String key = key(sourceNpcId, targetNpcId);
        NpcSocialState before = data.edges.getOrDefault(key, NpcSocialState.NEUTRAL);
        NpcSocialState after = before.apply(proposed, maxDeltaPerMutation);
        if (before.equals(after)) {
            return new NpcSocialGraphMutation(
                    NpcSocialGraphMutation.Status.NO_CHANGE,
                    sourceNpcId,
                    targetNpcId,
                    before,
                    after
            );
        }

        if (before.isNeutral()
                && !after.isNeutral()
                && outgoingEdgeCount(sourceNpcId) >= MAX_OUTGOING_EDGES_PER_NPC) {
            return new NpcSocialGraphMutation(
                    NpcSocialGraphMutation.Status.CAPACITY_REACHED,
                    sourceNpcId,
                    targetNpcId,
                    before,
                    before
            );
        }

        if (after.isNeutral()) {
            data.edges.remove(key);
        } else {
            data.edges.put(key, after);
        }
        save();
        return new NpcSocialGraphMutation(
                NpcSocialGraphMutation.Status.APPLIED,
                sourceNpcId,
                targetNpcId,
                before,
                after
        );
    }

    public synchronized NpcSocialCausalMutation applyCausalDelta(
            UUID sourceNpcId,
            UUID targetNpcId,
            UUID causeEventId,
            long causeGameTime,
            NpcSocialDelta proposed,
            int maxDeltaPerMutation
    ) {
        long safeGameTime = Math.max(0L, causeGameTime);
        if (!validPair(sourceNpcId, targetNpcId) || causeEventId == null) {
            return causalResult(
                    NpcSocialCausalMutation.Status.INVALID_PAIR,
                    null,
                    sourceNpcId,
                    targetNpcId,
                    causeEventId,
                    safeGameTime,
                    NpcSocialDelta.NONE,
                    NpcSocialDelta.NONE,
                    NpcSocialState.NEUTRAL,
                    NpcSocialState.NEUTRAL
            );
        }
        if (blockedCausalSources.contains(sourceNpcId)) {
            return causalResult(
                    NpcSocialCausalMutation.Status.FRONTIER_CORRUPT,
                    NpcSocialMutationIdentity.forCause(sourceNpcId, causeEventId),
                    sourceNpcId,
                    targetNpcId,
                    causeEventId,
                    safeGameTime,
                    canonicalRequest(proposed, maxDeltaPerMutation),
                    NpcSocialDelta.NONE,
                    get(sourceNpcId, targetNpcId),
                    get(sourceNpcId, targetNpcId)
            );
        }

        UUID mutationId = NpcSocialMutationIdentity.forCause(sourceNpcId, causeEventId);
        NpcSocialDelta boundedRequest = canonicalRequest(proposed, maxDeltaPerMutation);
        NpcSocialMutationCursor current = data.causalFrontiers.get(sourceNpcId.toString());
        if (current != null) {
            if (causeEventId.equals(current.causeEventId())) {
                if (sameReplayPayload(current, mutationId, targetNpcId, safeGameTime, boundedRequest)) {
                    return NpcSocialCausalMutation.fromCursor(
                            NpcSocialCausalMutation.Status.REPLAYED,
                            current
                    );
                }
                return causalResult(
                        NpcSocialCausalMutation.Status.CONFLICTING_CAUSE,
                        mutationId,
                        sourceNpcId,
                        targetNpcId,
                        causeEventId,
                        safeGameTime,
                        boundedRequest,
                        NpcSocialDelta.NONE,
                        get(sourceNpcId, targetNpcId),
                        get(sourceNpcId, targetNpcId)
                );
            }
            if (compareCause(safeGameTime, causeEventId, current) <= 0) {
                return causalResult(
                        NpcSocialCausalMutation.Status.STALE_CAUSE,
                        mutationId,
                        sourceNpcId,
                        targetNpcId,
                        causeEventId,
                        safeGameTime,
                        boundedRequest,
                        NpcSocialDelta.NONE,
                        get(sourceNpcId, targetNpcId),
                        get(sourceNpcId, targetNpcId)
                );
            }
        }

        String edgeKey = key(sourceNpcId, targetNpcId);
        NpcSocialState before = data.edges.getOrDefault(edgeKey, NpcSocialState.NEUTRAL);
        NpcSocialState after = before.apply(boundedRequest, NpcSocialState.MAX_VALUE);

        NpcSocialMutationCursor.Outcome outcome;
        NpcSocialCausalMutation.Status status;
        if (before.equals(after)) {
            outcome = NpcSocialMutationCursor.Outcome.NO_CHANGE;
            status = NpcSocialCausalMutation.Status.NO_CHANGE;
        } else if (before.isNeutral()
                && !after.isNeutral()
                && outgoingEdgeCount(sourceNpcId) >= MAX_OUTGOING_EDGES_PER_NPC) {
            after = before;
            outcome = NpcSocialMutationCursor.Outcome.CAPACITY_REACHED;
            status = NpcSocialCausalMutation.Status.CAPACITY_REACHED;
        } else {
            if (after.isNeutral()) {
                data.edges.remove(edgeKey);
            } else {
                data.edges.put(edgeKey, after);
            }
            outcome = NpcSocialMutationCursor.Outcome.APPLIED;
            status = NpcSocialCausalMutation.Status.APPLIED;
        }

        NpcSocialDelta appliedDelta = deltaBetween(before, after);
        NpcSocialMutationCursor cursor = new NpcSocialMutationCursor(
                mutationId,
                sourceNpcId,
                targetNpcId,
                causeEventId,
                safeGameTime,
                boundedRequest,
                appliedDelta,
                before,
                after,
                outcome
        );
        data.causalFrontiers.put(sourceNpcId.toString(), cursor);
        save();
        return NpcSocialCausalMutation.fromCursor(status, cursor);
    }

    private static boolean sameReplayPayload(
            NpcSocialMutationCursor current,
            UUID mutationId,
            UUID targetNpcId,
            long causeGameTime,
            NpcSocialDelta boundedRequest
    ) {
        return mutationId.equals(current.mutationId())
                && targetNpcId.equals(current.targetNpcId())
                && causeGameTime == current.causeGameTime()
                && boundedRequest.equals(current.boundedRequestedDelta());
    }

    private static int compareCause(
            long causeGameTime,
            UUID causeEventId,
            NpcSocialMutationCursor current
    ) {
        int time = Long.compare(causeGameTime, current.causeGameTime());
        if (time != 0) return time;
        return causeEventId.toString().compareTo(current.causeEventId().toString());
    }

    private static NpcSocialDelta canonicalRequest(NpcSocialDelta proposed, int maxDeltaPerMutation) {
        if (proposed == null) return NpcSocialDelta.NONE;
        return proposed.sanitized(maxDeltaPerMutation).sanitized(NpcSocialState.MAX_VALUE);
    }

    private static NpcSocialDelta deltaBetween(NpcSocialState before, NpcSocialState after) {
        return new NpcSocialDelta(
                after.trust() - before.trust(),
                after.respect() - before.respect(),
                after.fear() - before.fear(),
                after.affinity() - before.affinity()
        );
    }

    private static NpcSocialCausalMutation causalResult(
            NpcSocialCausalMutation.Status status,
            UUID mutationId,
            UUID sourceNpcId,
            UUID targetNpcId,
            UUID causeEventId,
            long causeGameTime,
            NpcSocialDelta boundedRequestedDelta,
            NpcSocialDelta appliedDelta,
            NpcSocialState before,
            NpcSocialState after
    ) {
        return new NpcSocialCausalMutation(
                status,
                mutationId,
                sourceNpcId,
                targetNpcId,
                causeEventId,
                causeGameTime,
                boundedRequestedDelta,
                appliedDelta,
                before,
                after
        );
    }

    private long outgoingEdgeCount(UUID sourceNpcId) {
        String prefix = sourceNpcId + "/";
        return data.edges.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isNeutral())
                .count();
    }

    private GraphFile load() {
        GraphFile loaded = JsonStoreRecovery.loadOrRecover(
                file,
                CODEC,
                value -> value != null
                        && value.version == FORMAT_VERSION
                        && value.edges != null,
                GraphFile::new
        );
        loaded.edges = sanitizeEdges(loaded.edges);
        if (loaded.causalFrontiers == null) {
            loaded.causalFrontiers = new HashMap<>();
        }
        return loaded;
    }

    private static Map<String, NpcSocialState> sanitizeEdges(Map<String, NpcSocialState> rawEdges) {
        if (rawEdges == null || rawEdges.isEmpty()) return new HashMap<>();

        Map<String, NpcSocialState> sanitized = new HashMap<>();
        Set<String> seenCanonicalKeys = new HashSet<>();
        Set<String> conflictedCanonicalKeys = new HashSet<>();

        rawEdges.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey() == null ? "" : entry.getKey()))
                .forEach(entry -> {
                    EdgePair pair = parseKey(entry.getKey());
                    if (pair == null) return;

                    String canonicalKey = key(pair.sourceNpcId(), pair.targetNpcId());
                    if (!seenCanonicalKeys.add(canonicalKey)) {
                        conflictedCanonicalKeys.add(canonicalKey);
                        sanitized.remove(canonicalKey);
                        return;
                    }
                    if (conflictedCanonicalKeys.contains(canonicalKey)) return;

                    NpcSocialState raw = entry.getValue();
                    if (raw == null) return;
                    NpcSocialState bounded = new NpcSocialState(
                            raw.trust(),
                            raw.respect(),
                            raw.fear(),
                            raw.affinity()
                    );
                    if (bounded.isNeutral()) return;
                    sanitized.put(canonicalKey, bounded);
                });

        for (String conflict : conflictedCanonicalKeys) {
            sanitized.remove(conflict);
        }

        Map<UUID, Integer> outgoingCounts = new HashMap<>();
        for (String canonicalKey : sanitized.keySet()) {
            EdgePair pair = parseKey(canonicalKey);
            if (pair != null) {
                outgoingCounts.merge(pair.sourceNpcId(), 1, Integer::sum);
            }
        }
        Set<UUID> overCapacitySources = new HashSet<>();
        for (Map.Entry<UUID, Integer> count : outgoingCounts.entrySet()) {
            if (count.getValue() > MAX_OUTGOING_EDGES_PER_NPC) {
                overCapacitySources.add(count.getKey());
            }
        }
        if (!overCapacitySources.isEmpty()) {
            sanitized.entrySet().removeIf(entry -> {
                EdgePair pair = parseKey(entry.getKey());
                return pair != null && overCapacitySources.contains(pair.sourceNpcId());
            });
        }

        return sanitized;
    }

    private void save() {
        JsonStoreRecovery.writeAtomic(file, CODEC, data);
    }

    private static boolean validPair(UUID sourceNpcId, UUID targetNpcId) {
        return sourceNpcId != null
                && targetNpcId != null
                && !sourceNpcId.equals(targetNpcId);
    }

    private static EdgePair parseKey(String raw) {
        if (raw == null || raw.isBlank()) return null;
        int separator = raw.indexOf('/');
        if (separator <= 0 || separator != raw.lastIndexOf('/') || separator == raw.length() - 1) {
            return null;
        }
        try {
            UUID sourceNpcId = UUID.fromString(raw.substring(0, separator));
            UUID targetNpcId = UUID.fromString(raw.substring(separator + 1));
            if (!validPair(sourceNpcId, targetNpcId)) return null;
            return new EdgePair(sourceNpcId, targetNpcId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String key(UUID sourceNpcId, UUID targetNpcId) {
        return sourceNpcId + "/" + targetNpcId;
    }

    private record EdgePair(UUID sourceNpcId, UUID targetNpcId) {
    }

    private static final class GraphFile {
        int version = FORMAT_VERSION;
        Map<String, NpcSocialState> edges = new HashMap<>();
        Map<String, NpcSocialMutationCursor> causalFrontiers = new HashMap<>();
    }
}
