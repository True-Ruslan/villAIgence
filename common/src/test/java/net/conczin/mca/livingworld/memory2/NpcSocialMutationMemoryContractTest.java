package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSocialMutationMemoryContractTest {
    @TempDir
    Path tempDir;

    @Test
    void evidenceKeepsExactStructuredSocialMutationState() {
        UUID mutation = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID target = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID cause = UUID.fromString("33333333-3333-3333-3333-333333333333");

        NpcSocialMutationEvidence evidence = new NpcSocialMutationEvidence(
                mutation,
                target,
                cause,
                500L,
                new NpcSocialDelta(150, -150, 4, -4),
                new NpcSocialDelta(7, -3, 2, -1),
                new NpcSocialState(90, -90, 10, -10),
                new NpcSocialState(97, -93, 12, -11)
        );

        assertEquals(mutation, evidence.mutationId());
        assertEquals(target, evidence.targetNpcId());
        assertEquals(cause, evidence.causeEventId());
        assertEquals(500L, evidence.causeGameTime());
        assertEquals(new NpcSocialDelta(100, -100, 4, -4), evidence.boundedRequestedDelta());
        assertEquals(new NpcSocialDelta(7, -3, 2, -1), evidence.appliedDelta());
        assertEquals(new NpcSocialState(90, -90, 10, -10), evidence.before());
        assertEquals(new NpcSocialState(97, -93, 12, -11), evidence.after());
    }

    @Test
    void historicalMemoryEventConstructorStillDefaultsSocialEvidenceToNull() {
        MemoryEvent historicalShape = new MemoryEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                MemoryEvent.Type.OBSERVATION,
                "historical observation",
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                20L,
                40,
                0,
                80,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertNull(historicalShape.npcSocialMutation());
    }

    @Test
    void npcSocialChangeHasRelationshipChangeRetentionContribution() {
        assertEquals(
                MemoryEventRetentionPolicy.typeContribution(MemoryEvent.Type.RELATIONSHIP_CHANGE),
                MemoryEventRetentionPolicy.typeContribution(MemoryEvent.Type.NPC_SOCIAL_CHANGE)
        );
    }

    @Test
    void genericMemoryPromptExcludesNpcSocialChangeBeforeAllocation() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID cause = UUID.randomUUID();
        UUID mutation = UUID.randomUUID();
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

        MemoryEvent social = new MemoryEvent(
                mutation,
                npc,
                MemoryEvent.Type.NPC_SOCIAL_CHANGE,
                "SOCIAL_CHANGE_MUST_NOT_ENTER_GENERIC_PROMPT",
                List.of(npc, target),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                200L,
                200L,
                100,
                100,
                100,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                new NpcSocialMutationEvidence(
                        mutation,
                        target,
                        cause,
                        199L,
                        new NpcSocialDelta(3, 0, 0, 1),
                        new NpcSocialDelta(3, 0, 0, 1),
                        NpcSocialState.NEUTRAL,
                        new NpcSocialState(3, 0, 0, 1)
                )
        );
        MemoryEvent ordinary = new MemoryEvent(
                UUID.randomUUID(),
                npc,
                MemoryEvent.Type.OBSERVATION,
                "ordinary visible observation",
                List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                201L,
                201L,
                80,
                0,
                100,
                List.of()
        );
        store.append(social, 256);
        store.append(ordinary, 256);

        List<String> context = Memory2ContextProvider.load(tempDir, npc, player, 202L);
        String rendered = String.join("\n", context);

        assertTrue(rendered.contains("ordinary visible observation"));
        assertFalse(rendered.contains("SOCIAL_CHANGE_MUST_NOT_ENTER_GENERIC_PROMPT"));
    }
}
