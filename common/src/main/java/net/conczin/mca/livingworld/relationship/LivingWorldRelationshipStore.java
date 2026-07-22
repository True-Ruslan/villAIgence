package net.conczin.mca.livingworld.relationship;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Persistent relationship state keyed by NPC UUID and player UUID. */
public final class LivingWorldRelationshipStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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
        if (!Files.exists(file)) return new RelationshipFile();
        try {
            RelationshipFile loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), RelationshipFile.class);
            if (loaded == null || loaded.version != FORMAT_VERSION) return new RelationshipFile();
            if (loaded.relationships == null) loaded.relationships = new HashMap<>();
            loaded.relationships.replaceAll((key, state) -> state == null ? LivingWorldRelationshipState.NEUTRAL : state);
            return loaded;
        } catch (IOException | RuntimeException e) {
            return new RelationshipFile();
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(
                    temp,
                    GSON.toJson(data),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to persist LivingWorld relationships to " + file, e);
        }
    }

    private static String key(UUID villagerId, UUID playerId) {
        return villagerId + "/" + playerId;
    }

    private static final class RelationshipFile {
        int version = FORMAT_VERSION;
        Map<String, LivingWorldRelationshipState> relationships = new HashMap<>();
    }
}
