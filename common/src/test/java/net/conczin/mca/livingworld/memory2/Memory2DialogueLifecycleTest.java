package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Memory2DialogueLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void successfulAnswerPersistsOneDialogueEvent() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        Memory2DialogueLifecycle.recordSuccessful(
                true,
                tempDir,
                npc,
                player,
                100L,
                "hello",
                Optional.of("hi there"),
                16,
                1_000L
        );

        List<MemoryEvent> memories = MemoryEventStore.forWorld(tempDir).getRecent(npc, 16);
        assertEquals(1, memories.size());
        MemoryEvent event = memories.getFirst();
        assertEquals(MemoryEvent.Type.DIALOGUE, event.type());
        assertEquals(MemoryEvent.Provenance.PLAYER_TOLD, event.provenance());
        assertEquals("Player said: hello | NPC replied: hi there", event.summary());
        assertEquals(List.of(npc, player), event.participants());
    }

    @Test
    void absentBlankNullOrDisabledAnswerPersistsNothing() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        Memory2DialogueLifecycle.recordSuccessful(true, tempDir, npc, player, 100L, "one", Optional.empty(), 16, 1_000L);
        Memory2DialogueLifecycle.recordSuccessful(true, tempDir, npc, player, 101L, "two", Optional.of("   "), 16, 2_000L);
        Memory2DialogueLifecycle.recordSuccessful(true, tempDir, npc, player, 102L, "three", null, 16, 3_000L);
        Memory2DialogueLifecycle.recordSuccessful(false, tempDir, npc, player, 103L, "four", Optional.of("reply"), 16, 4_000L);

        assertEquals(List.of(), MemoryEventStore.forWorld(tempDir).getRecent(npc, 16));
    }

    @Test
    void replayUsesExistingDeterministicDialogueIdentity() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        Memory2DialogueLifecycle.recordSuccessful(
                true, tempDir, npc, player, 100L,
                "I saw a dragon", Optional.of("Really?"), 16, 1_000L
        );
        Memory2DialogueLifecycle.recordSuccessful(
                true, tempDir, npc, player, 100L,
                "  I saw   a dragon  ", Optional.of("Tell me more."), 16, 9_999L
        );

        List<MemoryEvent> memories = MemoryEventStore.forWorld(tempDir).getRecent(npc, 16);
        assertEquals(1, memories.size());
        assertEquals("Player said: I saw a dragon | NPC replied: Really?", memories.getFirst().summary());
    }
}