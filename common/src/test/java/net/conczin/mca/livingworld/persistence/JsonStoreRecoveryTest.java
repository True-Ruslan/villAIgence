package net.conczin.mca.livingworld.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonStoreRecoveryTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @TempDir
    Path directory;

    @Test
    void truncatedJsonIsBackedUpAndCanonicalStoreIsRegenerated() throws IOException {
        Path file = directory.resolve("memory.json");
        byte[] corrupt = "{\"version\":1,\"values\":".getBytes();
        Files.write(file, corrupt);

        FixtureFile loaded = load(file);

        assertEquals(1, loaded.version);
        assertTrue(loaded.values.isEmpty());
        assertArrayEquals(corrupt, Files.readAllBytes(corruptBackup(file)));
        assertValidCanonical(file);
    }

    @Test
    void emptyFileIsBackedUpAndCanonicalStoreIsRegenerated() throws IOException {
        Path file = directory.resolve("memory2.json");
        Files.write(file, new byte[0]);

        FixtureFile loaded = load(file);

        assertTrue(loaded.values.isEmpty());
        assertEquals(0L, Files.size(corruptBackup(file)));
        assertValidCanonical(file);
    }

    @Test
    void wrongRootTypeIsRecovered() throws IOException {
        Path file = directory.resolve("semantic-memory.json");
        Files.writeString(file, "[]");

        FixtureFile loaded = load(file);

        assertTrue(loaded.values.isEmpty());
        assertEquals("[]", Files.readString(corruptBackup(file)));
        assertValidCanonical(file);
    }

    @Test
    void incompatibleSchemaIsRecovered() throws IOException {
        Path file = directory.resolve("relationships.json");
        Files.writeString(file, "{\"version\":2,\"values\":{\"old\":\"state\"}}");

        FixtureFile loaded = load(file);

        assertEquals(1, loaded.version);
        assertTrue(loaded.values.isEmpty());
        assertTrue(Files.readString(corruptBackup(file)).contains("\"version\":2"));
        assertValidCanonical(file);
    }

    @Test
    void staleTemporaryFileCannotOverrideValidCanonicalState() throws IOException {
        Path file = directory.resolve("voices.json");
        writeFixture(file, Map.of("canonical", "voice-a"));
        writeFixture(temporary(file), Map.of("stale", "voice-b"));
        byte[] canonical = Files.readAllBytes(file);

        FixtureFile loaded = load(file);

        assertEquals(Map.of("canonical", "voice-a"), loaded.values);
        assertArrayEquals(canonical, Files.readAllBytes(file));
        assertFalse(Files.exists(temporary(file)));
        assertFalse(Files.exists(corruptBackup(file)));
    }

    @Test
    void validOrphanTemporaryFileCompletesInterruptedAtomicCommit() throws IOException {
        Path file = directory.resolve("operator-lore.json");
        writeFixture(temporary(file), Map.of("world", "canonical lore"));

        FixtureFile loaded = load(file);

        assertEquals(Map.of("world", "canonical lore"), loaded.values);
        assertTrue(Files.isRegularFile(file));
        assertFalse(Files.exists(temporary(file)));
        assertFalse(Files.exists(temporaryCorruptBackup(file)));
    }

    @Test
    void invalidOrphanTemporaryFileIsBackedUpAndEmptyCanonicalStoreIsCreated()
            throws IOException {
        Path file = directory.resolve("operator-lore.json");
        Files.writeString(temporary(file), "{broken");

        FixtureFile loaded = load(file);

        assertTrue(loaded.values.isEmpty());
        assertEquals("{broken", Files.readString(temporaryCorruptBackup(file)));
        assertValidCanonical(file);
    }

    @Test
    void secondLoadIsIdempotentAndDoesNotRewriteRecoveryEvidence() throws IOException {
        Path file = directory.resolve("memory.json");
        byte[] corrupt = "null".getBytes();
        Files.write(file, corrupt);

        FixtureFile first = load(file);
        byte[] canonicalAfterFirstLoad = Files.readAllBytes(file);
        byte[] backupAfterFirstLoad = Files.readAllBytes(corruptBackup(file));
        FixtureFile second = load(file);

        assertEquals(first.values, second.values);
        assertArrayEquals(canonicalAfterFirstLoad, Files.readAllBytes(file));
        assertArrayEquals(backupAfterFirstLoad, Files.readAllBytes(corruptBackup(file)));
    }

    @Test
    void recoveringOneStoreDoesNotMutateSiblingStore() throws IOException {
        Path corruptStore = directory.resolve("memory.json");
        Path healthyStore = directory.resolve("relationships.json");
        Files.writeString(corruptStore, "{broken");
        writeFixture(healthyStore, Map.of("npc", "trusted"));
        byte[] healthyBefore = Files.readAllBytes(healthyStore);

        load(corruptStore);

        assertArrayEquals(healthyBefore, Files.readAllBytes(healthyStore));
        assertFalse(Files.exists(corruptBackup(healthyStore)));
    }

    private static FixtureFile load(Path file) {
        return JsonStoreRecovery.loadOrRecover(
                file,
                GSON,
                FixtureFile.class,
                value -> value != null && value.version == 1 && value.values != null,
                FixtureFile::new
        );
    }

    private static void writeFixture(Path path, Map<String, String> values) throws IOException {
        FixtureFile fixture = new FixtureFile();
        fixture.values.putAll(values);
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(fixture));
    }

    private static void assertValidCanonical(Path file) throws IOException {
        FixtureFile canonical = GSON.fromJson(Files.readString(file), FixtureFile.class);
        assertEquals(1, canonical.version);
        assertTrue(canonical.values.isEmpty());
    }

    private static Path temporary(Path file) {
        return file.resolveSibling(file.getFileName() + ".tmp");
    }

    private static Path corruptBackup(Path file) {
        return file.resolveSibling(file.getFileName() + ".corrupt");
    }

    private static Path temporaryCorruptBackup(Path file) {
        return file.resolveSibling(file.getFileName() + ".tmp.corrupt");
    }

    static final class FixtureFile {
        int version = 1;
        Map<String, String> values = new HashMap<>();
    }
}
