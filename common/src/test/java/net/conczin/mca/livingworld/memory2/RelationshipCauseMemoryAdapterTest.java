package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipChange;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationshipCauseMemoryAdapterTest {
    @Test
    void mapsMatchingPersistedEvidenceToDeterministicGenericCause() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent change = relationshipChange(npc, player, 100L);
        MemoryEvent dialogue = dialogue(npc, player, 100L, "I protected the village.", "I noticed that.");

        MemoryEvent first = RelationshipCauseMemoryAdapter.toDialogueTurnCause(change, dialogue, player).orElseThrow();
        MemoryEvent replay = RelationshipCauseMemoryAdapter.toDialogueTurnCause(change, dialogue, player).orElseThrow();

        assertEquals(first.id(), replay.id());
        assertEquals(npc, first.ownerNpcId());
        assertEquals(MemoryEvent.Type.RELATIONSHIP_CAUSE, first.type());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, first.provenance());
        assertEquals(List.of(npc, player), first.participants());
        assertEquals(100, first.confidence());
        assertEquals(List.of(), first.relationshipReasons());
        assertEquals("Relationship change occurred during dialogue with player.", first.summary());
        assertEquals(MemoryEvent.CauseKind.DIALOGUE_TURN, first.relationshipCause().kind());
        assertEquals(change.id(), first.relationshipCause().relationshipChangeEventId());
        assertEquals(dialogue.id(), first.relationshipCause().evidenceEventId());
        assertEquals(change.relationshipTransition(), first.relationshipCause().transitionSnapshot());
        assertTrue(first.createdAtEpochMillis() > change.createdAtEpochMillis());
        assertTrue(first.createdAtEpochMillis() > dialogue.createdAtEpochMillis());
        assertFalse(first.summary().contains(dialogue.dialogue().playerMessage()));
        assertFalse(first.summary().contains(dialogue.dialogue().npcReply()));
    }

    @Test
    void rejectsDifferentDialogueTurnForSameNpcAndPlayer() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent change = relationshipChange(npc, player, 100L);
        MemoryEvent previousDialogue = dialogue(npc, player, 99L, "older turn", "older reply");
        MemoryEvent laterDialogue = dialogue(npc, player, 101L, "later turn", "later reply");

        assertTrue(RelationshipCauseMemoryAdapter.toDialogueTurnCause(change, previousDialogue, player).isEmpty());
        assertTrue(RelationshipCauseMemoryAdapter.toDialogueTurnCause(change, laterDialogue, player).isEmpty());
    }

    @Test
    void rejectsMismatchedOwnersPlayersTypesAndMissingTransitionPayload() {
        UUID npc = UUID.randomUUID();
        UUID otherNpc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        MemoryEvent change = relationshipChange(npc, player, 100L);
        MemoryEvent validDialogue = dialogue(npc, player, 100L, "hello", "hi");
        MemoryEvent otherOwnerDialogue = dialogue(otherNpc, player, 100L, "hello", "hi");
        MemoryEvent otherPlayerDialogue = dialogue(npc, otherPlayer, 100L, "hello", "hi");
        MemoryEvent nonDialogue = observation(npc, player, 100L);
        MemoryEvent nonRelationship = observation(npc, player, 99L);
        MemoryEvent missingTransition = new MemoryEvent(
                UUID.randomUUID(),
                npc,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                "Legacy relationship change.",
                List.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L,
                1000L,
                55,
                0,
                100,
                List.of()
        );

        assertTrue(RelationshipCauseMemoryAdapter.toDialogueTurnCause(null, validDialogue, player).isEmpty());
        assertTrue(RelationshipCauseMemoryAdapter.toDialogueTurnCause(change, null, player).isEmpty());
        assertTrue(RelationshipCauseMemoryAdapter.toDialogueTurnCause(change, validDialogue, null).isEmpty());
        assertTrue(RelationshipCauseMemoryAdapter.toDialogueTurnCause(change, otherOwnerDialogue, player).isEmpty());
        assertTrue(RelationshipCauseMemoryAdapter.toDialogueTurnCause(change, otherPlayerDialogue, player).isEmpty());
        assertTrue(RelationshipCauseMemoryAdapter.toDialogueTurnCause(change, nonDialogue, player).isEmpty());
        assertTrue(RelationshipCauseMemoryAdapter.toDialogueTurnCause(nonRelationship, validDialogue, player).isEmpty());
        assertTrue(RelationshipCauseMemoryAdapter.toDialogueTurnCause(missingTransition, validDialogue, player).isEmpty());
    }

    private static MemoryEvent relationshipChange(UUID npc, UUID player, long gameTime) {
        return RelationshipChangeMemoryAdapter.toMemoryEvent(
                npc,
                player,
                gameTime,
                LivingWorldRelationshipChange.between(
                        new LivingWorldRelationshipState(1, 2, 3, 4),
                        new LivingWorldRelationshipState(3, 1, 2, 5)
                ),
                1_000L
        ).orElseThrow();
    }

    private static MemoryEvent dialogue(UUID npc, UUID player, long gameTime, String playerMessage, String npcReply) {
        return new MemoryEvent(
                UUID.randomUUID(),
                npc,
                MemoryEvent.Type.DIALOGUE,
                "Dialogue with player.",
                List.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                2_000L,
                40,
                0,
                100,
                List.of(),
                new MemoryEvent.DialogueExchange(playerMessage, npcReply)
        );
    }

    private static MemoryEvent observation(UUID npc, UUID player, long gameTime) {
        return new MemoryEvent(
                UUID.randomUUID(),
                npc,
                MemoryEvent.Type.OBSERVATION,
                "Observed event.",
                List.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                3_000L,
                30,
                0,
                100,
                List.of()
        );
    }
}
