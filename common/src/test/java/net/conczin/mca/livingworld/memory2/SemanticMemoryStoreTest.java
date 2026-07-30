package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SemanticMemoryStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsEntriesSeparatesNpcsAndIgnoresDuplicateIds() {
        Path file = tempDir.resolve("semantic-memory.json");
        UUID npcA = UUID.randomUUID();
        UUID npcB = UUID.randomUUID();
        UUID sharedId = UUID.randomUUID();

        SemanticMemoryStore first = new SemanticMemoryStore(file);
        first.append(entry(sharedId, npcA, 100L, "A-first"), 8);
        first.append(entry(sharedId, npcA, 200L, "duplicate-must-not-replace"), 8);
        first.append(entry(UUID.randomUUID(), npcB, 300L, "B-only"), 8);

        SemanticMemoryStore reloaded = new SemanticMemoryStore(file);

        assertEquals(List.of("A-first"), reloaded.getRecent(npcA, 8).stream().map(SemanticMemoryEntry::statement).toList());
        assertEquals(List.of("B-only"), reloaded.getRecent(npcB, 8).stream().map(SemanticMemoryEntry::statement).toList());
    }

    @Test
    void boundsRetentionPerNpcKeepingNewestEntries() {
        SemanticMemoryStore store = new SemanticMemoryStore(tempDir.resolve("semantic-memory.json"));
        UUID npc = UUID.randomUUID();

        store.append(entry(UUID.randomUUID(), npc, 100L, "one"), 2);
        store.append(entry(UUID.randomUUID(), npc, 200L, "two"), 2);
        store.append(entry(UUID.randomUUID(), npc, 300L, "three"), 2);

        assertEquals(List.of("three", "two"), store.getRecent(npc, 10).stream().map(SemanticMemoryEntry::statement).toList());
    }

    @Test
    void consolidatesIndependentEvidenceBeforeRetentionAndPersistsIt() {
        Path file = tempDir.resolve("semantic-memory.json");
        SemanticMemoryStore store = new SemanticMemoryStore(file);
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();
        UUID sourceB = UUID.randomUUID();
        UUID sourceC = UUID.randomUUID();

        store.append(sourcedEntry(UUID.randomUUID(), npc, 100L, "Village gate open", List.of(npc, player), sourceA), 2);
        store.append(sourcedEntry(UUID.randomUUID(), npc, 200L, "village   gate\topen", List.of(player, npc), sourceB), 2);
        store.append(sourcedEntry(UUID.randomUUID(), npc, 300L, "Bell repaired", List.of(npc), sourceC), 2);

        SemanticMemoryStore reloaded = new SemanticMemoryStore(file);
        List<SemanticMemoryEntry> entries = reloaded.getRecent(npc, 10);
        assertEquals(2, entries.size());
        SemanticMemoryEntry gate = entries.stream()
                .filter(value -> value.statement().toLowerCase(Locale.ROOT).contains("village gate open"))
                .findFirst()
                .orElseThrow();
        assertEquals(sortedIds(sourceA, sourceB), gate.sourceEventIds());
    }

    @Test
    void replayDuplicateDoesNotRewriteFile() throws Exception {
        Path file = tempDir.resolve("semantic-memory.json");
        SemanticMemoryStore store = new SemanticMemoryStore(file);
        UUID npc = UUID.randomUUID();
        SemanticMemoryEntry value = sourcedEntry(
                UUID.randomUUID(), npc, 100L, "Stable fact", List.of(npc), UUID.randomUUID()
        );

        store.append(value, 8);
        byte[] before = Files.readAllBytes(file);
        store.append(value, 8);
        byte[] after = Files.readAllBytes(file);

        assertArrayEquals(before, after);
    }

    @Test
    void reloadConsolidatesCompatiblePersistedEntriesInMemory() throws Exception {
        Path file = tempDir.resolve("semantic-memory.json");
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();
        UUID sourceB = UUID.randomUUID();
        SemanticMemoryEntry first = sourcedEntry(
                UUID.randomUUID(), npc, 100L, "Market is open", List.of(npc, player), sourceA
        );
        SemanticMemoryEntry second = sourcedEntry(
                UUID.randomUUID(), npc, 200L, "market is open", List.of(player, npc), sourceB
        );
        Files.writeString(file, semanticFileJson(npc, List.of(first, second)), StandardCharsets.UTF_8);

        SemanticMemoryStore reloaded = new SemanticMemoryStore(file);
        List<SemanticMemoryEntry> entries = reloaded.getRecent(npc, 8);

        assertEquals(1, entries.size());
        assertEquals(sortedIds(sourceA, sourceB), entries.getFirst().sourceEventIds());
    }

    @Test
    void malformedFileFailsOpenAndIsReplacedOnNextAppend() throws Exception {
        Path file = tempDir.resolve("semantic-memory.json");
        Files.writeString(file, "{broken", StandardCharsets.UTF_8);
        UUID npc = UUID.randomUUID();

        SemanticMemoryStore store = new SemanticMemoryStore(file);
        assertEquals(List.of(), store.getRecent(npc, 8));

        store.append(entry(UUID.randomUUID(), npc, 100L, "recovered"), 8);

        SemanticMemoryStore reloaded = new SemanticMemoryStore(file);
        assertEquals(List.of("recovered"), reloaded.getRecent(npc, 8).stream().map(SemanticMemoryEntry::statement).toList());
    }

    private static String semanticFileJson(UUID npc, List<SemanticMemoryEntry> entries) {
        String values = entries.stream()
                .map(SemanticMemoryStoreTest::entryJson)
                .collect(Collectors.joining(","));
        return "{\"version\":1,\"entriesByNpc\":{\"" + npc + "\":[" + values + "]}}";
    }

    private static String entryJson(SemanticMemoryEntry entry) {
        return "{"
                + "\"id\":\"" + entry.id() + "\","
                + "\"ownerNpcId\":\"" + entry.ownerNpcId() + "\","
                + "\"kind\":\"" + entry.kind() + "\","
                + "\"statement\":\"" + escapeJson(entry.statement()) + "\","
                + "\"relatedEntities\":" + idArrayJson(entry.relatedEntities()) + ","
                + "\"provenance\":\"" + entry.provenance() + "\","
                + "\"gameTime\":" + entry.gameTime() + ","
                + "\"createdAtEpochMillis\":" + entry.createdAtEpochMillis() + ","
                + "\"importance\":" + entry.importance() + ","
                + "\"confidence\":" + entry.confidence() + ","
                + "\"sourceEventIds\":" + idArrayJson(entry.sourceEventIds())
                + "}";
    }

    private static String idArrayJson(List<UUID> values) {
        return values.stream()
                .map(value -> "\"" + value + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static SemanticMemoryEntry entry(UUID id, UUID owner, long gameTime, String statement) {
        return new SemanticMemoryEntry(
                id,
                owner,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(owner),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                1_700_000_000_000L + gameTime,
                70,
                100,
                List.of()
        );
    }

    private static SemanticMemoryEntry sourcedEntry(
            UUID id,
            UUID owner,
            long gameTime,
            String statement,
            List<UUID> relatedEntities,
            UUID sourceId
    ) {
        return new SemanticMemoryEntry(
                id,
                owner,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                relatedEntities,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                1_700_000_000_000L + gameTime,
                70,
                100,
                List.of(sourceId)
        );
    }

    private static List<UUID> sortedIds(UUID... ids) {
        List<UUID> values = new ArrayList<>(List.of(ids));
        values.sort(Comparator.comparing(UUID::toString));
        return List.copyOf(values);
    }
}
