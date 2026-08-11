package net.conczin.mca.livingworld.relationship;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.conczin.mca.livingworld.persistence.GsonJsonStoreCodec;
import net.conczin.mca.livingworld.persistence.JsonStoreRecovery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalStateException("Relationship store root must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            if (requiredInt(root, "version") != FORMAT_VERSION) {
                throw new IllegalStateException("Unsupported relationship store format");
            }

            JsonElement relationshipsElement = root.get("relationships");
            if (relationshipsElement == null || !relationshipsElement.isJsonObject()) {
                throw new IllegalStateException("Relationship store relationships must be an object");
            }

            Set<String> canonicalKeys = new HashSet<>();
            LivingWorldRelationshipState requested = null;
            for (Map.Entry<String, JsonElement> entry : relationshipsElement.getAsJsonObject().entrySet()) {
                RelationshipPair pair = parsePair(entry.getKey());
                String canonicalKey = key(pair.villagerId(), pair.playerId());
                if (!canonicalKeys.add(canonicalKey)) {
                    throw new IllegalStateException("Duplicate canonical relationship pair: " + canonicalKey);
                }

                LivingWorldRelationshipState state = parseState(entry.getValue());
                if (pair.villagerId().equals(villagerId) && pair.playerId().equals(playerId)) {
                    requested = state;
                }
            }
            return requested == null ? LivingWorldRelationshipState.NEUTRAL : requested;
        } catch (IOException | RuntimeException e) {
            if (e instanceof IllegalStateException illegalState) {
                throw illegalState;
            }
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

    private static LivingWorldRelationshipState parseState(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalStateException("Relationship state must be an object");
        }
        JsonObject state = element.getAsJsonObject();
        return new LivingWorldRelationshipState(
                boundedStateValue(state, "trust"),
                boundedStateValue(state, "respect"),
                boundedStateValue(state, "fear"),
                boundedStateValue(state, "affinity")
        );
    }

    private static int boundedStateValue(JsonObject object, String field) {
        int value = requiredInt(object, field);
        if (value < -100 || value > 100) {
            throw new IllegalStateException("Relationship field out of range: " + field);
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

    private static RelationshipPair parsePair(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Relationship key is blank");
        }
        int separator = raw.indexOf('/');
        if (separator <= 0 || separator != raw.lastIndexOf('/') || separator == raw.length() - 1) {
            throw new IllegalStateException("Relationship key is malformed: " + raw);
        }
        try {
            UUID villager = UUID.fromString(raw.substring(0, separator));
            UUID player = UUID.fromString(raw.substring(separator + 1));
            if (!raw.equals(key(villager, player))) {
                throw new IllegalStateException("Relationship key is not canonical: " + raw);
            }
            return new RelationshipPair(villager, player);
        } catch (IllegalArgumentException e) {
            if (e instanceof IllegalStateException illegalState) {
                throw illegalState;
            }
            throw new IllegalStateException("Relationship key contains an invalid UUID: " + raw, e);
        }
    }

    private static String key(UUID villagerId, UUID playerId) {
        return villagerId + "/" + playerId;
    }

    private record RelationshipPair(UUID villagerId, UUID playerId) {
    }

    private static final class RelationshipFile {
        int version = FORMAT_VERSION;
        Map<String, LivingWorldRelationshipState> relationships = new HashMap<>();
    }
}
