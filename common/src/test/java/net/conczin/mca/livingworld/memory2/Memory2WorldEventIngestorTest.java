package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.knowledge.WorldEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Memory2WorldEventIngestorTest {
    @TempDir
    Path tempDir;

    @Test
    void duplicateSourceEventIsIdempotent() {
        UUID actor = UUID.randomUUID();
        WorldEvent event = event(UUID.randomUUID(), actor, 10L, "first");

        Memory2WorldEventIngestor.record(tempDir, event, 10, 1_700_000_000_000L);
        Memory2WorldEventIngestor.record(tempDir, event, 10, 1_700_000_000_999L);

        List<MemoryEvent> memories = MemoryEventStore.forWorld(tempDir).getRecent(actor, 10);
        assertEquals(1, memories.size());
        assertEquals(event.id(), memories.getFirst().id());
        assertEquals(1_700_000_000_000L, memories.getFirst().createdAtEpochMillis());
    }

    @Test
    void distinctEventsRemainDistinctAndRetentionIsBounded() {
        UUID actor = UUID.randomUUID();
        WorldEvent first = event(UUID.randomUUID(), actor, 10L, "first");
        WorldEvent second = event(UUID.randomUUID(), actor, 20L, "second");

        Memory2WorldEventIngestor.record(tempDir, first, 1, 1L);
        Memory2WorldEventIngestor.record(tempDir, second, 1, 2L);

        List<MemoryEvent> memories = MemoryEventStore.forWorld(tempDir).getRecent(actor, 10);
        assertEquals(List.of(second.id()), memories.stream().map(MemoryEvent::id).toList());
    }

    @Test
    void missingActorDoesNotPersistAnything() {
        WorldEvent invalid = new WorldEvent(
                UUID.randomUUID(), WorldEvent.Type.NPC_ACTION, "event",
                WorldEvent.Provenance.SYSTEM_OBSERVED, "minecraft:overworld",
                0, 64, 0, 10L, null, UUID.randomUUID()
        );

        Memory2WorldEventIngestor.record(tempDir, invalid, 10, 1L);

        assertEquals(List.of(), MemoryEventStore.forWorld(tempDir).getRecent(UUID.randomUUID(), 10));
    }

    @Test
    void enabledSemanticIngestionWritesLinkedFactIdempotently() {
        UUID actor = UUID.randomUUID();
        WorldEvent event = event(UUID.randomUUID(), actor, 10L, "performed an action");

        Memory2WorldEventIngestor.record(tempDir, event, 10, true, 10, 100L);
        Memory2WorldEventIngestor.record(tempDir, event, 10, true, 10, 999L);

        List<MemoryEvent> episodes = MemoryEventStore.forWorld(tempDir).getRecent(actor, 10);
        List<SemanticMemoryEntry> semantics = SemanticMemoryStore.forWorld(tempDir).getRecent(actor, 10);
        assertEquals(1, episodes.size());
        assertEquals(1, semantics.size());
        assertEquals(SemanticMemoryEntry.Kind.FACT, semantics.getFirst().kind());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, semantics.getFirst().provenance());
        assertEquals(List.of(event.id()), semantics.getFirst().sourceEventIds());
        assertEquals("performed an action", semantics.getFirst().statement());
    }

    @Test
    void disabledSemanticIngestionLeavesEpisodicBehaviorUnchanged() {
        UUID actor = UUID.randomUUID();
        WorldEvent event = event(UUID.randomUUID(), actor, 10L, "episodic only");

        Memory2WorldEventIngestor.record(tempDir, event, 10, false, 10, 100L);

        assertEquals(1, MemoryEventStore.forWorld(tempDir).getRecent(actor, 10).size());
        assertEquals(List.of(), SemanticMemoryStore.forWorld(tempDir).getRecent(actor, 10));
    }

    @Test
    void semanticFailureCannotRollbackEpisodicWrite() throws Exception {
        UUID actor = UUID.randomUUID();
        WorldEvent event = event(UUID.randomUUID(), actor, 10L, "durable episode");
        Files.createDirectories(tempDir.resolve("livingworld").resolve("semantic-memory.json"));

        try {
            Memory2WorldEventIngestor.record(tempDir, event, 10, true, 10, 100L);
        } catch (RuntimeException ignored) {
            // Semantic persistence is auxiliary; either propagation or local handling is acceptable.
        }

        assertEquals(List.of(event.id()), MemoryEventStore.forWorld(tempDir).getRecent(actor, 10)
                .stream().map(MemoryEvent::id).toList());
    }

    private static WorldEvent event(UUID id, UUID actor, long gameTime, String description) {
        return new WorldEvent(
                id,
                WorldEvent.Type.NPC_ACTION,
                description,
                WorldEvent.Provenance.SYSTEM_OBSERVED,
                "minecraft:overworld",
                0, 64, 0,
                gameTime,
                actor,
                UUID.randomUUID()
        );
    }
}
