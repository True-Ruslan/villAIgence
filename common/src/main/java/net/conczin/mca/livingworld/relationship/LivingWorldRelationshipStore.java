package net.conczin.mca.livingworld.relationship;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.conczin.mca.livingworld.persistence.GsonJsonStoreCodec;
import net.conczin.mca.livingworld.persistence.JsonStoreRecovery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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

    /**
     * Read-only authorization view of the canonical relationship file.
     * Missing persistence is neutral; malformed, symlinked, or non-regular persistence fails closed by exception.
     * This path intentionally bypasses JsonStoreRecovery so an authorization check can never mutate or repair state.
     */
    public static LivingWorldRelationshipState readStrict(
            Path worldRoot,
            UUID villagerId,
            UUID playerId
    ) {
        if (worldRoot == null || villagerId == null || playerId == null) {
            throw new IllegalArgumentException("worldRoot, villagerId and playerId are required");
        }

        Path file = worldRoot.toAbsolutePath().normalize().resolve("livingworld").resolve("relationships.json");
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return LivingWorldRelationshipState.NEUTRAL;
        }
        if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Relationship store is not a regular file: " + file);
        }

        try {
            RelationshipFile loaded = CODEC.decode(Files.readString(file, StandardCharsets.UTF_8));
            if (loaded == null || loaded.version != FORMAT_VERSION || loaded.relationships == null) {
                throw new IllegalStateException("Relationship store schema is invalid: " + file);
            }
            LivingWorldRelationshipState state = loaded.relationships.get(key(villagerId, playerId));
            return state == null ? LivingWorldRelationshipState.NEUTRAL : state;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Unable to read relationship store strictly: " + file, e);
        }
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
