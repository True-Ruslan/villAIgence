package net.conczin.mca.livingworld.relationship;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Strict read-only view of directed NPC social edges for authorization/gating decisions.
 *
 * <p>This reader deliberately does not use {@link NpcSocialGraphStore#forWorld(Path)} because the normal
 * store owns recovery semantics. A behavior gate must not turn corrupt social authority into an empty/neutral
 * graph and accidentally authorize behavior. Missing persistence is neutral; malformed or unsafe persistence
 * fails closed by exception and is never repaired or rewritten here.</p>
 */
public final class NpcSocialGraphStrictPairReader {
    private static final int FORMAT_VERSION = 1;
    private static final int MAX_OUTGOING_EDGES_PER_NPC = 64;

    private NpcSocialGraphStrictPairReader() {
    }

    public static NpcSocialState read(
            Path worldRoot,
            UUID sourceNpcId,
            UUID targetNpcId
    ) {
        return readMany(worldRoot, sourceNpcId, List.of(targetNpcId)).get(targetNpcId);
    }

    /**
     * Strictly validates the complete persisted graph once and returns exact directed state for the
     * requested targets. Missing persistence/edges are neutral; malformed persistence fails closed.
     */
    public static Map<UUID, NpcSocialState> readMany(
            Path worldRoot,
            UUID sourceNpcId,
            Collection<UUID> targetNpcIds
    ) {
        if (worldRoot == null || sourceNpcId == null || targetNpcIds == null) {
            throw new IllegalArgumentException("worldRoot, source NPC and target NPC ids are required");
        }

        Set<UUID> requestedTargets = new LinkedHashSet<>();
        for (UUID targetNpcId : targetNpcIds) {
            if (targetNpcId == null
                    || sourceNpcId.equals(targetNpcId)
                    || !requestedTargets.add(targetNpcId)) {
                throw new IllegalArgumentException("target NPC ids must be distinct, non-null and differ from source");
            }
        }
        if (requestedTargets.isEmpty()) {
            return Map.of();
        }

        Map<UUID, NpcSocialState> requestedStates = new LinkedHashMap<>();
        requestedTargets.forEach(targetNpcId -> requestedStates.put(targetNpcId, NpcSocialState.NEUTRAL));

        Path file = worldRoot.toAbsolutePath().normalize()
                .resolve("livingworld")
                .resolve("npc-social-graph.json");
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return Collections.unmodifiableMap(requestedStates);
        }
        if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("NPC social graph is not a regular file: " + file);
        }

        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalStateException("NPC social graph root must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            if (requiredInt(root, "version") != FORMAT_VERSION) {
                throw new IllegalStateException("Unsupported NPC social graph format");
            }

            JsonElement edgesElement = root.get("edges");
            if (edgesElement == null || !edgesElement.isJsonObject()) {
                throw new IllegalStateException("NPC social graph edges must be an object");
            }

            Set<String> canonicalKeys = new HashSet<>();
            Map<UUID, Integer> outgoingCounts = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : edgesElement.getAsJsonObject().entrySet()) {
                EdgePair pair = parsePair(entry.getKey());
                String canonicalKey = pair.sourceNpcId() + "/" + pair.targetNpcId();
                if (!canonicalKeys.add(canonicalKey)) {
                    throw new IllegalStateException("Duplicate canonical NPC social edge: " + canonicalKey);
                }

                NpcSocialState state = parseState(entry.getValue());
                if (state.isNeutral()) {
                    throw new IllegalStateException("Neutral NPC social edges must not be persisted");
                }
                int count = outgoingCounts.merge(pair.sourceNpcId(), 1, Integer::sum);
                if (count > MAX_OUTGOING_EDGES_PER_NPC) {
                    throw new IllegalStateException("NPC social graph source exceeds outgoing edge capacity");
                }

                if (pair.sourceNpcId().equals(sourceNpcId)
                        && requestedTargets.contains(pair.targetNpcId())) {
                    requestedStates.put(pair.targetNpcId(), state);
                }
            }
            return Collections.unmodifiableMap(requestedStates);
        } catch (IOException | RuntimeException e) {
            if (e instanceof IllegalStateException illegalState) {
                throw illegalState;
            }
            throw new IllegalStateException("Unable to read NPC social graph strictly: " + file, e);
        }
    }

    private static NpcSocialState parseState(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalStateException("NPC social edge state must be an object");
        }
        JsonObject state = element.getAsJsonObject();
        return new NpcSocialState(
                boundedStateValue(state, "trust"),
                boundedStateValue(state, "respect"),
                boundedStateValue(state, "fear"),
                boundedStateValue(state, "affinity")
        );
    }

    private static int boundedStateValue(JsonObject object, String field) {
        int value = requiredInt(object, field);
        if (value < NpcSocialState.MIN_VALUE || value > NpcSocialState.MAX_VALUE) {
            throw new IllegalStateException("NPC social edge field out of range: " + field);
        }
        return value;
    }

    private static int requiredInt(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException("Required integer field is missing or invalid: " + field);
        }
        String raw = element.getAsString();
        if (!raw.matches("-?(0|[1-9][0-9]*)")) {
            throw new IllegalStateException("Required integer field is not canonical: " + field);
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Required integer field is out of range: " + field, e);
        }
    }

    private static EdgePair parsePair(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("NPC social edge key is blank");
        }
        int separator = raw.indexOf('/');
        if (separator <= 0 || separator != raw.lastIndexOf('/') || separator == raw.length() - 1) {
            throw new IllegalStateException("NPC social edge key is malformed: " + raw);
        }
        UUID source;
        UUID target;
        try {
            source = UUID.fromString(raw.substring(0, separator));
            target = UUID.fromString(raw.substring(separator + 1));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("NPC social edge key contains an invalid UUID: " + raw, e);
        }
        if (source.equals(target)) {
            throw new IllegalStateException("NPC social self-edge is invalid");
        }
        String canonical = source + "/" + target;
        if (!raw.equals(canonical)) {
            throw new IllegalStateException("NPC social edge key is not canonical: " + raw);
        }
        return new EdgePair(source, target);
    }

    private record EdgePair(UUID sourceNpcId, UUID targetNpcId) {
    }
}
