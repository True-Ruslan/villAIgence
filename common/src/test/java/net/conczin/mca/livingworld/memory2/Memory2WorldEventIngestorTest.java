package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.knowledge.WorldEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
