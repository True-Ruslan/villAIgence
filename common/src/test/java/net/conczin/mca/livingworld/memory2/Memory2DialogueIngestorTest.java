package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// RED contract intentionally references production types that do not exist yet.
class Memory2DialogueIngestorTest {
    @TempDir
    Path tempDir;

    @Test
    void duplicateReplayIsIdempotentEvenWhenReplyAndWallClockDiffer() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        Memory2DialogueIngestor.record(
                tempDir, npc, player, 100L, "I saw a dragon", "Really?", 16, 1_000L
        );
        Memory2DialogueIngestor.record(
                tempDir, npc, player, 100L, "  I saw   a dragon  ", "Tell me more.", 16, 9_999L
        );

        List<MemoryEvent> memories = MemoryEventStore.forWorld(tempDir).getRecent(npc, 16);
        assertEquals(1, memories.size());
        assertEquals("Player said: I saw a dragon | NPC replied: Really?", memories.getFirst().summary());
    }

    @Test
    void distinctTurnsRemainDistinctAndRetentionIsBounded() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        Memory2DialogueIngestor.record(tempDir, npc, player, 100L, "turn one", "reply one", 2, 1_000L);
        Memory2DialogueIngestor.record(tempDir, npc, player, 101L, "turn two", "reply two", 2, 2_000L);
        Memory2DialogueIngestor.record(tempDir, npc, player, 102L, "turn three", "reply three", 2, 3_000L);

        List<MemoryEvent> memories = MemoryEventStore.forWorld(tempDir).getRecent(npc, 16);
        assertEquals(2, memories.size());
        assertTrue(memories.get(0).summary().contains("turn three"));
        assertTrue(memories.get(1).summary().contains("turn two"));
    }

    @Test
    void invalidDialogueCreatesNoPersistentMemory() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        Memory2DialogueIngestor.record(tempDir, npc, player, 100L, "hello", "   ", 16, 1_000L);

        assertEquals(List.of(), MemoryEventStore.forWorld(tempDir).getRecent(npc, 16));
    }
}
