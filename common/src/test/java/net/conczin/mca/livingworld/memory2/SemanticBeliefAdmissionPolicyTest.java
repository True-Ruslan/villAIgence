package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticBeliefAdmissionPolicyTest {
    @Test
    void admitsPlayerToldClaimFromMatchingDialogueEvidence() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent dialogue = dialogue(
                UUID.randomUUID(),
                npc,
                player,
                MemoryEvent.Provenance.PLAYER_TOLD,
                42L
        );

        SemanticBeliefSource admitted = SemanticBeliefAdmissionPolicy.admit(
                dialogue,
                "The north bridge is unsafe.",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                70,
                65
        ).orElseThrow();

        assertEquals(npc, admitted.ownerNpcId());
        assertEquals("The north bridge is unsafe.", admitted.statement());
        assertEquals(List.of(player), admitted.relatedEntities());
        assertEquals(MemoryEvent.Provenance.PLAYER_TOLD, admitted.provenance());
        assertEquals(42L, admitted.gameTime());
        assertEquals(List.of(dialogue.id()), admitted.sourceEventIds());
    }

    @Test
    void admitsNpcToldClaimOnlyFromNpcToldDialogueEvidence() {
        UUID npc = UUID.randomUUID();
        UUID otherNpc = UUID.randomUUID();
        MemoryEvent dialogue = dialogue(
                UUID.randomUUID(),
                npc,
                otherNpc,
                MemoryEvent.Provenance.NPC_TOLD,
                50L
        );

        assertTrue(SemanticBeliefAdmissionPolicy.admit(
                dialogue,
                "The miller says the road is blocked.",
                List.of(otherNpc),
                MemoryEvent.Provenance.NPC_TOLD,
                55,
                60
        ).isPresent());

        assertTrue(SemanticBeliefAdmissionPolicy.admit(
                dialogue,
                "The miller says the road is blocked.",
                List.of(otherNpc),
                MemoryEvent.Provenance.PLAYER_TOLD,
                55,
                60
        ).isEmpty());
    }

    @Test
    void inferredClaimRemainsBeliefAndKeepsExplicitSourceEvidence() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent dialogue = dialogue(
                UUID.randomUUID(),
                npc,
                player,
                MemoryEvent.Provenance.PLAYER_TOLD,
                60L
        );

        SemanticBeliefSource admitted = SemanticBeliefAdmissionPolicy.admit(
                dialogue,
                "The player may be avoiding the east road.",
                List.of(player),
                MemoryEvent.Provenance.INFERRED,
                40,
                35
        ).orElseThrow();

        assertEquals(MemoryEvent.Provenance.INFERRED, admitted.provenance());
        assertEquals(List.of(dialogue.id()), admitted.sourceEventIds());
    }

    @Test
    void rejectsUnsupportedOrUntrustworthyAdmissionInputs() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent playerDialogue = dialogue(
                UUID.randomUUID(),
                npc,
                player,
                MemoryEvent.Provenance.PLAYER_TOLD,
                70L
        );
        MemoryEvent observedAction = new MemoryEvent(
                UUID.randomUUID(),
                npc,
                MemoryEvent.Type.ACTION,
                "NPC opened a door.",
                List.of(npc),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                71L,
                1_700_000_000_071L,
                60,
                0,
                100,
                List.of()
        );

        assertTrue(SemanticBeliefAdmissionPolicy.admit(
                playerDialogue,
                "",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                50,
                50
        ).isEmpty());
        assertTrue(SemanticBeliefAdmissionPolicy.admit(
                playerDialogue,
                "Claim",
                List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                50,
                100
        ).isEmpty());
        assertTrue(SemanticBeliefAdmissionPolicy.admit(
                observedAction,
                "The player said the door is dangerous.",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                50,
                50
        ).isEmpty());
    }

    private static MemoryEvent dialogue(
            UUID id,
            UUID npc,
            UUID speaker,
            MemoryEvent.Provenance provenance,
            long gameTime
    ) {
        return new MemoryEvent(
                id,
                npc,
                MemoryEvent.Type.DIALOGUE,
                "Dialogue evidence",
                List.of(npc, speaker),
                provenance,
                gameTime,
                1_700_000_000_000L + gameTime,
                40,
                0,
                60,
                List.of(),
                new MemoryEvent.DialogueExchange("Source utterance", "NPC reply")
        );
    }
}
