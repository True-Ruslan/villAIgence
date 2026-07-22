package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

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
}
