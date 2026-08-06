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

/** Persistent relationship state keyed by NPC UUID and player UUID. */
public final class LivingWorldRelationshipStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonStoreRecovery.Codec<RelationshipFile> CODEC =
            new GsonJsonStoreCodec<>(GSON, RelationshipFile.class);
    private static final ConcurrentMap<Path, LivingWorldRelationshipStore> STORES = new ConcurrentHashMap<>();

    private final Path file;
    private RelationshipFile data;

    public static LivingWorldRelationshipStore forWorld(Path worldRoot) {
        Path file = worldRoot.toAbsolutePath().normalize().resolve("livingworld").resolve("relationships.json");
        return STORES.computeIfAbsent(file, LivingWorldRelationshipStore::new);
    }

    LivingWorldRelationshipStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
        this.data = load();
    }

    public synchronized LivingWorldRelationshipState get(UUID villagerId, UUID playerId) {
        if (villagerId == null || playerId == null) return LivingWorldRelationshipState.NEUTRAL;
        return data.relationships.getOrDefault(key(villagerId, playerId), LivingWorldRelationshipState.NEUTRAL);
    }

    public synchronized LivingWorldRelationshipState applyDelta(
            UUID villagerId,
            UUID playerId,
            LivingWorldRelationshipDelta proposed,
            int maxDeltaPerTurn
    ) {
        return applyDeltaWithResult(villagerId, playerId, proposed, maxDeltaPerTurn).after();
    }

    public synchronized LivingWorldRelationshipChange applyDeltaWithResult(
            UUID villagerId,
            UUID playerId,
            LivingWorldRelationshipDelta proposed,
            int maxDeltaPerTurn
    ) {
        LivingWorldRelationshipState before = get(villagerId, playerId);
        if (villagerId == null || playerId == null || proposed == null) {
            return LivingWorldRelationshipChange.between(before, before);
        }

        LivingWorldRelationshipState after = before.apply(proposed, maxDeltaPerTurn);
        if (!after.equals(before)) {
            data.relationships.put(key(villagerId, playerId), after);
            save();
        }
        return LivingWorldRelationshipChange.between(before, after);
    }

    private RelationshipFile load() {
        RelationshipFile loaded = JsonStoreRecovery.loadOrRecover(
                file,
                CODEC,
                value -> value != null
                        && value.version == FORMAT_VERSION
                        && value.relationships != null,
                RelationshipFile::new
        );
        loaded.relationships.replaceAll(
                (key, state) -> state == null
                        ? LivingWorldRelationshipState.NEUTRAL
                        : state
        );
        return loaded;
    }

    private void save() {
        JsonStoreRecovery.writeAtomic(file, CODEC, data);
    }

    private static String key(UUID villagerId, UUID playerId) {
        return villagerId + "/" + playerId;
    }

    private static final class RelationshipFile {
        int version = FORMAT_VERSION;
        Map<String, LivingWorldRelationshipState> relationships = new HashMap<>();
    }
}
