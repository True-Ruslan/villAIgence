package net.conczin.mca.livingworld.lore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.conczin.mca.livingworld.persistence.JsonStoreRecovery;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * World-local persistence for explicit server/operator-authored lore.
 *
 * <p>This store is deliberately separate from observed world facts, episodic memory and semantic memory.</p>
 */
public final class WorldOperatorLoreStore {
    public static final int MAX_CODE_POINTS = 4_096;

    private static final int VERSION = 1;
    private static final String DIRECTORY = "livingworld";
    private static final String FILE_NAME = "operator-lore.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final JsonStoreRecovery.Codec<JsonObject> CODEC =
            new JsonStoreRecovery.Codec<>() {
                @Override
                public JsonObject decode(String raw) {
                    JsonElement element = JsonParser.parseString(raw);
                    if (!element.isJsonObject()) {
                        throw new IllegalStateException(
                                "operator lore root must be a JSON object"
                        );
                    }
                    return element.getAsJsonObject();
                }

                @Override
                public String encode(JsonObject value) {
                    return GSON.toJson(value);
                }
            };

    private final Path file;
    private final TreeMap<String, String> villagers = new TreeMap<>();
    private final TreeMap<String, String> players = new TreeMap<>();
    private final TreeMap<String, String> villages = new TreeMap<>();
    private String world = "";

    private WorldOperatorLoreStore(Path worldRoot) {
        Path root = Objects.requireNonNull(worldRoot, "worldRoot")
                .toAbsolutePath()
                .normalize();
        this.file = root.resolve(DIRECTORY).resolve(FILE_NAME);
        load();
    }

    public static WorldOperatorLoreStore forWorld(Path worldRoot) {
        return new WorldOperatorLoreStore(worldRoot);
    }

    public synchronized String get(OperatorLoreKey key) {
        Objects.requireNonNull(key, "key");
        return switch (key.scope()) {
            case WORLD -> world;
            case VILLAGER -> villagers.getOrDefault(key.storageKey(), "");
            case PLAYER -> players.getOrDefault(key.storageKey(), "");
            case VILLAGE -> villages.getOrDefault(key.storageKey(), "");
        };
    }

    public synchronized void put(OperatorLoreKey key, String value) {
        Objects.requireNonNull(key, "key");
        String normalized = normalizeValue(value);
        if (normalized.isBlank()) {
            normalized = "";
        }

        String previous = get(key);
        if (previous.equals(normalized)) {
            return;
        }

        apply(key, normalized);
        try {
            save();
        } catch (RuntimeException failure) {
            apply(key, previous);
            throw failure;
        }
    }

    public synchronized OperatorLoreSnapshot snapshot(
            String dimension,
            UUID villagerId,
            UUID playerId,
            int villageId
    ) {
        String villagerLore = villagerId == null
                ? ""
                : get(OperatorLoreKey.villager(villagerId));
        String playerLore = playerId == null
                ? ""
                : get(OperatorLoreKey.player(playerId));
        String villageLore = dimension == null || dimension.isBlank()
                ? ""
                : get(OperatorLoreKey.village(dimension, villageId));
        return new OperatorLoreSnapshot(world, villagerLore, playerLore, villageLore);
    }

    private void apply(OperatorLoreKey key, String value) {
        switch (key.scope()) {
            case WORLD -> world = value;
            case VILLAGER -> updateMap(villagers, key.storageKey(), value);
            case PLAYER -> updateMap(players, key.storageKey(), value);
            case VILLAGE -> updateMap(villages, key.storageKey(), value);
        }
    }

    private static void updateMap(Map<String, String> map, String key, String value) {
        if (value.isEmpty()) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    private void load() {
        JsonObject root = JsonStoreRecovery.loadOrRecover(
                file,
                CODEC,
                WorldOperatorLoreStore::isValidRoot,
                WorldOperatorLoreStore::emptyRoot
        );
        clearState();
        world = normalizeValue(readString(root, "world"));
        readMap(root, "villagers", villagers);
        readMap(root, "players", players);
        readMap(root, "villages", villages);
    }

    private static boolean isValidRoot(JsonObject root) {
        try {
            validateVersion(root);
            readString(root, "world");
            readMap(root, "villagers", new TreeMap<>());
            readMap(root, "players", new TreeMap<>());
            readMap(root, "villages", new TreeMap<>());
            return true;
        } catch (RuntimeException malformed) {
            return false;
        }
    }

    private static void validateVersion(JsonObject root) {
        JsonElement versionElement = root.get("version");
        if (versionElement == null
                || !versionElement.isJsonPrimitive()
                || !versionElement.getAsJsonPrimitive().isNumber()
                || versionElement.getAsInt() != VERSION) {
            throw new IllegalStateException("unsupported operator lore schema version");
        }
    }

    private static JsonObject emptyRoot() {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        root.addProperty("world", "");
        root.add("villagers", new JsonObject());
        root.add("players", new JsonObject());
        root.add("villages", new JsonObject());
        return root;
    }

    private void clearState() {
        world = "";
        villagers.clear();
        players.clear();
        villages.clear();
    }

    private static String readString(JsonObject root, String field) {
        JsonElement value = root.get(field);
        if (value == null || value.isJsonNull()) {
            return "";
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalStateException(field + " must be a string");
        }
        return value.getAsString();
    }

    private static void readMap(
            JsonObject root,
            String field,
            Map<String, String> destination
    ) {
        JsonElement value = root.get(field);
        if (value == null || value.isJsonNull()) {
            return;
        }
        if (!value.isJsonObject()) {
            throw new IllegalStateException(field + " must be an object");
        }

        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
            JsonElement lore = entry.getValue();
            if (!lore.isJsonPrimitive() || !lore.getAsJsonPrimitive().isString()) {
                throw new IllegalStateException(field + " values must be strings");
            }
            String normalized = normalizeValue(lore.getAsString());
            if (!normalized.isBlank()) {
                destination.put(entry.getKey(), normalized);
            }
        }
    }

    private void save() {
        try {
            JsonStoreRecovery.writeAtomic(file, CODEC, toJson());
        } catch (UncheckedIOException failure) {
            throw new IllegalStateException("Unable to save operator lore", failure);
        }
    }

    private JsonObject toJson() {
        JsonObject root = emptyRoot();
        root.addProperty("world", world);
        root.add("villagers", toJsonObject(villagers));
        root.add("players", toJsonObject(players));
        root.add("villages", toJsonObject(villages));
        return root;
    }

    private static JsonObject toJsonObject(Map<String, String> values) {
        JsonObject object = new JsonObject();
        values.forEach(object::addProperty);
        return object;
    }

    private static String normalizeValue(String value) {
        String normalized = value == null
                ? ""
                : value.replace("\r\n", "\n").replace('\r', '\n');

        normalized.codePoints().forEach(codePoint -> {
            if (codePoint < 0x20 && codePoint != '\n' && codePoint != '\t') {
                throw new IllegalArgumentException(
                        "operator lore contains forbidden control character U+"
                                + String.format("%04X", codePoint)
                );
            }
        });

        int codePoints = normalized.codePointCount(0, normalized.length());
        if (codePoints > MAX_CODE_POINTS) {
            int end = normalized.offsetByCodePoints(0, MAX_CODE_POINTS);
            normalized = normalized.substring(0, end);
        }
        return normalized;
    }
}
