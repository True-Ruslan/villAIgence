package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryEventStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsPerNpcAndKeepsNewestBoundedEvents() {
        UUID npcA = UUID.randomUUID();
        UUID npcB = UUID.randomUUID();
        Path file = tempDir.resolve("memory2.json");
        MemoryEventStore store = new MemoryEventStore(file);

        MemoryEvent a30 = event("a30", npcA, 30L);
        MemoryEvent a10 = event("a10", npcA, 10L);
        MemoryEvent a20 = event("a20", npcA, 20L);
        MemoryEvent b40 = event("b40", npcB, 40L);

        store.append(a30, 2);
        store.append(a10, 2);
        store.append(a20, 2);
        store.append(b40, 2);

        MemoryEventStore reloaded = new MemoryEventStore(file);
        assertEquals(List.of(a30.id(), a20.id()), reloaded.getRecent(npcA, 10).stream().map(MemoryEvent::id).toList());
        assertEquals(List.of(b40.id()), reloaded.getRecent(npcB, 10).stream().map(MemoryEvent::id).toList());
    }

    @Test
    void duplicateEventIdsAreIdempotent() {
        UUID npc = UUID.randomUUID();
        MemoryEventStore store = new MemoryEventStore(tempDir.resolve("memory2.json"));
        MemoryEvent event = event("same", npc, 10L);

        store.append(event, 10);
        store.append(event, 10);

        assertEquals(List.of(event.id()), store.getRecent(npc, 10).stream().map(MemoryEvent::id).toList());
    }

    @Test
    void corruptFileFailsOpenAndIsRepairedOnNextAppend() throws Exception {
        UUID npc = UUID.randomUUID();
        Path file = tempDir.resolve("memory2.json");
        Files.writeString(file, "{broken", StandardCharsets.UTF_8);

        MemoryEventStore store = new MemoryEventStore(file);
        assertEquals(List.of(), store.getRecent(npc, 10));

        MemoryEvent recovered = event("recovered", npc, 50L);
        store.append(recovered, 10);

        MemoryEventStore reloaded = new MemoryEventStore(file);
        assertEquals(List.of(recovered.id()), reloaded.getRecent(npc, 10).stream().map(MemoryEvent::id).toList());
    }

    @Test
    void invalidQueriesReturnEmptyResults() {
        MemoryEventStore store = new MemoryEventStore(tempDir.resolve("memory2.json"));
        assertEquals(List.of(), store.getRecent(null, 10));
        assertEquals(List.of(), store.getRecent(UUID.randomUUID(), 0));
    }

    private static MemoryEvent event(String seed, UUID ownerNpcId, long gameTime) {
        return new MemoryEvent(
                UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)),
                ownerNpcId,
                MemoryEvent.Type.OBSERVATION,
                seed,
                List.of(ownerNpcId),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                1_700_000_000_000L + gameTime,
                75,
                20,
                100,
                List.of()
        );
    }
}
