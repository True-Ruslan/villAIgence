package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExactMemoryAuthorityLookupTest {
    @TempDir
    Path tempDir;

    @Test
    void exactEventLookupFindsOwnerEventRegardlessOfRecentPositionWithoutMutation() {
        UUID npc = UUID.fromString("00000000-0000-0000-0000-000000010001");
        UUID otherNpc = UUID.fromString("00000000-0000-0000-0000-000000010002");
        MemoryEventStore store = new MemoryEventStore(tempDir.resolve("memory2.json"));
        MemoryEvent old = event("authority-old", npc, 10L);
        store.append(old, 64);
        for (int i = 0; i < 40; i++) {
            store.append(event("authority-new-" + i, npc, 100L + i), 64);
        }
        List<MemoryEvent> before = store.getRecent(npc, 64);

        assertEquals(Optional.of(old), store.findById(npc, old.id()));
        assertEquals(Optional.empty(), store.findById(otherNpc, old.id()));
        assertEquals(Optional.empty(), store.findById(npc, UUID.randomUUID()));
        assertEquals(Optional.empty(), store.findById(null, old.id()));
        assertEquals(Optional.empty(), store.findById(npc, null));
        assertEquals(before, store.getRecent(npc, 64));
    }

    @Test
    void exactSemanticLookupAndOwnerLocalPredicateLookupDoNotDependOnRecencyOrMutateState() {
        SemanticMemoryStore store = new SemanticMemoryStore(tempDir.resolve("semantic-memory.json"));
        UUID npc = UUID.fromString("00000000-0000-0000-0000-000000010101");
        UUID otherNpc = UUID.fromString("00000000-0000-0000-0000-000000010102");
        SemanticMemoryEntry old = semantic(
                UUID.fromString("00000000-0000-0000-0000-000000010103"),
                npc,
                10L,
                "old exact semantic",
                UUID.fromString("00000000-0000-0000-0000-000000010104")
        );
        store.append(old, 64);
        for (int i = 0; i < 40; i++) {
            store.append(semantic(
                    new UUID(0L, 20_000L + i),
                    npc,
                    100L + i,
                    "new semantic " + i,
                    new UUID(0L, 30_000L + i)
            ), 64);
        }
        List<SemanticMemoryEntry> before = store.getRecent(npc, 64);

        assertEquals(Optional.of(old), store.findById(npc, old.id()));
        assertEquals(Optional.empty(), store.findById(otherNpc, old.id()));
        assertEquals(Optional.empty(), store.findById(npc, UUID.randomUUID()));
        assertEquals(Optional.empty(), store.findById(null, old.id()));
        assertEquals(Optional.empty(), store.findById(npc, null));
        assertEquals(Optional.of(old), store.findMatching(npc, value -> value.id().equals(old.id())));
        assertEquals(Optional.empty(), store.findMatching(otherNpc, value -> true));
        assertEquals(Optional.empty(), store.findMatching(npc, null));
        assertEquals(before, store.getRecent(npc, 64));
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

    private static SemanticMemoryEntry semantic(
            UUID id,
            UUID ownerNpcId,
            long gameTime,
            String statement,
            UUID sourceId
    ) {
        return new SemanticMemoryEntry(
                id,
                ownerNpcId,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                1_700_000_000_000L + gameTime,
                70,
                100,
                List.of(sourceId)
        );
    }
}
