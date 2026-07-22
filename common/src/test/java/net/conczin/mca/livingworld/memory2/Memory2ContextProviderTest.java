package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Memory2ContextProviderTest {
    @TempDir
    Path tempDir;

    @Test
    void returnsAtMostSixBoundedFormattedMemories() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

        for (int i = 0; i < 10; i++) {
            store.append(event(UUID.randomUUID(), npc, player, 100L + i, "memory-" + i, 50), 64);
        }

        List<String> context = Memory2ContextProvider.load(tempDir, npc, player, 200L);

        assertEquals(6, context.size());
    }

    @Test
    void currentPlayerParticipantRelevanceAffectsOrdering() {
        UUID npc = UUID.randomUUID();
        UUID currentPlayer = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

        store.append(event(UUID.randomUUID(), npc, otherPlayer, 100L, "other-player-memory", 70), 64);
        store.append(event(UUID.randomUUID(), npc, currentPlayer, 100L, "current-player-memory", 70), 64);

        List<String> context = Memory2ContextProvider.load(tempDir, npc, currentPlayer, 100L);

        assertTrue(context.getFirst().contains("current-player-memory"));
    }

    @Test
    void preservesNpcIsolationAndEmptyPath() {
        UUID npcA = UUID.randomUUID();
        UUID npcB = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);
        store.append(event(UUID.randomUUID(), npcB, player, 100L, "private-to-b", 100), 64);

        assertEquals(List.of(), Memory2ContextProvider.load(tempDir, npcA, player, 100L));
    }

    private static MemoryEvent event(
            UUID id,
            UUID owner,
            UUID participant,
            long gameTime,
            String summary,
            int importance
    ) {
        return new MemoryEvent(
                id,
                owner,
                MemoryEvent.Type.ACTION,
                summary,
                List.of(owner, participant),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                1_700_000_000_000L + gameTime,
                importance,
                0,
                100,
                List.of()
        );
    }
}
