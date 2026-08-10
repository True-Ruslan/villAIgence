package net.conczin.mca.livingworld.relationship;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.conczin.mca.livingworld.persistence.GsonJsonStoreCodec;
import net.conczin.mca.livingworld.persistence.JsonStoreRecovery;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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
        if (loaded.edges == null) loaded.edges = new HashMap<>();
        return loaded;
    }

    private void save() {
        JsonStoreRecovery.writeAtomic(file, CODEC, data);
    }

    private static boolean validPair(UUID sourceNpcId, UUID targetNpcId) {
        return sourceNpcId != null
                && targetNpcId != null
                && !sourceNpcId.equals(targetNpcId);
    }

    private static String key(UUID sourceNpcId, UUID targetNpcId) {
        return sourceNpcId + "/" + targetNpcId;
    }

    private static final class GraphFile {
        int version = FORMAT_VERSION;
        Map<String, NpcSocialState> edges = new HashMap<>();
    }
}
