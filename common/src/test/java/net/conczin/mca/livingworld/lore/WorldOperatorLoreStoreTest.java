package net.conczin.mca.livingworld.lore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldOperatorLoreStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void emptyStoreReturnsBlankScopes() {
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(tempDir);

        assertEquals("", store.get(OperatorLoreKey.world()));
        assertEquals("", store.get(OperatorLoreKey.villager(UUID.randomUUID())));
        assertEquals("", store.get(OperatorLoreKey.player(UUID.randomUUID())));
        assertEquals("", store.get(OperatorLoreKey.village("minecraft:overworld", 7)));
    }

    @Test
    void allScopesRoundTripAndSnapshotRemainSeparate() {
        UUID villager = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(tempDir);

        store.put(OperatorLoreKey.world(), "The kingdom is recovering.");
        store.put(OperatorLoreKey.villager(villager), "A retired cartographer.");
        store.put(OperatorLoreKey.player(player), "Known as the bridge builder.");
        store.put(OperatorLoreKey.village("minecraft:overworld", 42), "Founded beside an old fort.");

        OperatorLoreSnapshot snapshot = store.snapshot(
                "minecraft:overworld",
                villager,
                player,
                42
        );

        assertEquals("The kingdom is recovering.", snapshot.world());
        assertEquals("A retired cartographer.", snapshot.villager());
        assertEquals("Known as the bridge builder.", snapshot.player());
        assertEquals("Founded beside an old fort.", snapshot.village());
    }

    @Test
    void storesAreIsolatedByWorldRoot() {
        Path firstWorld = tempDir.resolve("first");
        Path secondWorld = tempDir.resolve("second");
        WorldOperatorLoreStore first = WorldOperatorLoreStore.forWorld(firstWorld);
        WorldOperatorLoreStore second = WorldOperatorLoreStore.forWorld(secondWorld);

        first.put(OperatorLoreKey.world(), "First world");
        second.put(OperatorLoreKey.world(), "Second world");

        assertEquals("First world", first.get(OperatorLoreKey.world()));
        assertEquals("Second world", second.get(OperatorLoreKey.world()));
    }

    @Test
    void villageKeysIncludeDimension() {
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(tempDir);
        store.put(OperatorLoreKey.village("minecraft:overworld", 12), "Overworld village");
        store.put(OperatorLoreKey.village("minecraft:the_nether", 12), "Nether village");

        assertEquals(
                "Overworld village",
                store.get(OperatorLoreKey.village("minecraft:overworld", 12))
        );
        assertEquals(
                "Nether village",
                store.get(OperatorLoreKey.village("minecraft:the_nether", 12))
        );
    }

    @Test
    void lineEndingsAreNormalizedAndNormalUnicodeIsPreserved() {
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(tempDir);
        store.put(OperatorLoreKey.world(), "Первая строка\r\nВторая строка\rТретья 🏰");

        assertEquals(
                "Первая строка\nВторая строка\nТретья 🏰",
                store.get(OperatorLoreKey.world())
        );
    }

    @Test
    void forbiddenControlCharactersAreRejected() {
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(tempDir);

        assertThrows(
                IllegalArgumentException.class,
                () -> store.put(OperatorLoreKey.world(), "valid\u0000invalid")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> store.put(OperatorLoreKey.world(), "valid\u0007invalid")
        );

        store.put(OperatorLoreKey.world(), "tab\tand\nnewline");
        assertEquals("tab\tand\nnewline", store.get(OperatorLoreKey.world()));
    }

    @Test
    void valuesAreBoundedByUnicodeCodePointsWithoutSplittingEmoji() {
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(tempDir);
        String value = "🏰".repeat(WorldOperatorLoreStore.MAX_CODE_POINTS + 1);

        store.put(OperatorLoreKey.world(), value);
        String stored = store.get(OperatorLoreKey.world());

        assertEquals(WorldOperatorLoreStore.MAX_CODE_POINTS, stored.codePointCount(0, stored.length()));
        assertTrue(stored.endsWith("🏰"));
    }

    @Test
    void exactReplayDoesNotRewriteTheFile() throws Exception {
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(tempDir);
        OperatorLoreKey key = OperatorLoreKey.world();
        store.put(key, "Stable lore");
        Path file = tempDir.resolve("livingworld/operator-lore.json");
        FileTime sentinel = FileTime.fromMillis(1_234L);
        Files.setLastModifiedTime(file, sentinel);

        store.put(key, "Stable lore");

        assertEquals(sentinel, Files.getLastModifiedTime(file));
    }

    @Test
    void blankValueRemovesScopedEntry() {
        UUID villager = UUID.randomUUID();
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(tempDir);
        OperatorLoreKey key = OperatorLoreKey.villager(villager);
        store.put(key, "Temporary lore");

        store.put(key, "   ");

        assertEquals("", store.get(key));
    }

    @Test
    void atomicSaveProducesVersionedInspectableJson() throws Exception {
        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(tempDir);
        store.put(OperatorLoreKey.world(), "World lore");
        Path file = tempDir.resolve("livingworld/operator-lore.json");

        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();

        assertEquals(1, root.get("version").getAsInt());
        assertEquals("World lore", root.get("world").getAsString());
        assertTrue(root.get("villagers").isJsonObject());
        assertTrue(root.get("players").isJsonObject());
        assertTrue(root.get("villages").isJsonObject());
        assertFalse(Files.exists(file.resolveSibling("operator-lore.json.tmp")));
    }

    @Test
    void malformedJsonFailsOpenAndPreservesBackup() throws Exception {
        Path directory = tempDir.resolve("livingworld");
        Files.createDirectories(directory);
        Path file = directory.resolve("operator-lore.json");
        Files.writeString(file, "{not-json", StandardCharsets.UTF_8);

        WorldOperatorLoreStore store = WorldOperatorLoreStore.forWorld(tempDir);

        assertEquals("", store.get(OperatorLoreKey.world()));
        assertTrue(Files.exists(directory.resolve("operator-lore.json.corrupt")));
        JsonObject recovered = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(1, recovered.get("version").getAsInt());
    }

    @Test
    void formatterUsesExplicitProvenanceLabelsAndSkipsBlankScopes() {
        OperatorLoreSnapshot snapshot = new OperatorLoreSnapshot(
                "World setting",
                "Villager history",
                "",
                "Village history"
        );

        List<String> lines = OperatorLoreFormatter.format(snapshot);

        assertEquals(List.of(
                "Server-authored world lore:\nWorld setting",
                "Server-authored villager lore:\nVillager history",
                "Server-authored village lore:\nVillage history"
        ), lines);
    }
}
