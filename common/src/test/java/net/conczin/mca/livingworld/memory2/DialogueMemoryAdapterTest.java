package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueMemoryAdapterTest {
    @Test
    void mapsSuccessfulDialogueToConservativeBeliefMemory() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        MemoryEvent event = DialogueMemoryAdapter.toMemoryEvent(
                npc,
                player,
                1234L,
                "  I found diamonds\nunder the house.  ",
                "  That sounds interesting.\tShow me later.  ",
                1_700_000_000_123L
        ).orElseThrow();

        assertEquals(npc, event.ownerNpcId());
        assertEquals(MemoryEvent.Type.DIALOGUE, event.type());
        assertEquals(MemoryEvent.Provenance.PLAYER_TOLD, event.provenance());
        assertEquals(List.of(npc, player), event.participants());
        assertEquals(1234L, event.gameTime());
        assertEquals(1_700_000_000_123L, event.createdAtEpochMillis());
        assertEquals(40, event.importance());
        assertEquals(0, event.emotionalWeight());
        assertEquals(60, event.confidence());
        assertEquals(List.of(), event.relationshipReasons());
        assertEquals(
                "Player said: I found diamonds under the house. | NPC replied: That sounds interesting. Show me later.",
                event.summary()
        );
    }

    @Test
    void boundsEachUtteranceByUnicodeCodePoints() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        String playerMessage = "🙂".repeat(300);
        String npcReply = "Ж".repeat(300);

        MemoryEvent event = DialogueMemoryAdapter.toMemoryEvent(
                npc, player, 10L, playerMessage, npcReply, 20L
        ).orElseThrow();

        String summary = event.summary();
        String prefix = "Player said: ";
        String separator = " | NPC replied: ";
        int separatorIndex = summary.indexOf(separator);
        String storedPlayer = summary.substring(prefix.length(), separatorIndex);
        String storedNpc = summary.substring(separatorIndex + separator.length());

        assertEquals(240, storedPlayer.codePointCount(0, storedPlayer.length()));
        assertEquals(240, storedNpc.codePointCount(0, storedNpc.length()));
        assertTrue(storedPlayer.endsWith("🙂"));
    }

    @Test
    void deterministicIdIdentifiesTurnNotReplyOrWallClock() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        MemoryEvent first = DialogueMemoryAdapter.toMemoryEvent(
                npc, player, 42L, "hello there", "first reply", 1000L
        ).orElseThrow();
        MemoryEvent replay = DialogueMemoryAdapter.toMemoryEvent(
                npc, player, 42L, "  hello   there  ", "different reply", 9999L
        ).orElseThrow();
        MemoryEvent laterTurn = DialogueMemoryAdapter.toMemoryEvent(
                npc, player, 43L, "hello there", "first reply", 1000L
        ).orElseThrow();
        MemoryEvent differentPlayerText = DialogueMemoryAdapter.toMemoryEvent(
                npc, player, 42L, "hello again", "first reply", 1000L
        ).orElseThrow();

        assertEquals(first.id(), replay.id());
        assertNotEquals(first.id(), laterTurn.id());
        assertNotEquals(first.id(), differentPlayerText.id());
    }

    @Test
    void rejectsMissingOrBlankTurnData() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        assertTrue(DialogueMemoryAdapter.toMemoryEvent(null, player, 1L, "hello", "reply", 1L).isEmpty());
        assertTrue(DialogueMemoryAdapter.toMemoryEvent(npc, null, 1L, "hello", "reply", 1L).isEmpty());
        assertTrue(DialogueMemoryAdapter.toMemoryEvent(npc, player, 1L, null, "reply", 1L).isEmpty());
        assertTrue(DialogueMemoryAdapter.toMemoryEvent(npc, player, 1L, "   ", "reply", 1L).isEmpty());
        assertTrue(DialogueMemoryAdapter.toMemoryEvent(npc, player, 1L, "hello", null, 1L).isEmpty());
        assertTrue(DialogueMemoryAdapter.toMemoryEvent(npc, player, 1L, "hello", "\n\t", 1L).isEmpty());
    }
}
