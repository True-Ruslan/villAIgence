package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialCausalMutation;
import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSocialMutationMemoryAdapterTest {
    @Test
    void appliedGraphMutationBecomesExactStructuredAuditEvent() {
        UUID source = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID target = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID cause = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID mutationId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        NpcSocialCausalMutation mutation = mutation(
                NpcSocialCausalMutation.Status.APPLIED,
                mutationId,
                source,
                target,
                cause,
                900L,
                new NpcSocialDelta(4, -2, 1, 3),
                new NpcSocialDelta(4, -2, 1, 3),
                NpcSocialState.NEUTRAL,
                new NpcSocialState(4, -2, 1, 3)
        );

        MemoryEvent event = NpcSocialMutationMemoryAdapter.toMemoryEvent(
                source,
                mutation,
                123456L
        ).orElseThrow();

        assertEquals(mutationId, event.id());
        assertEquals(source, event.ownerNpcId());
        assertEquals(MemoryEvent.Type.NPC_SOCIAL_CHANGE, event.type());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, event.provenance());
        assertEquals(900L, event.gameTime());
        assertEquals(123456L, event.createdAtEpochMillis());
        assertEquals(java.util.List.of(source, target), event.participants());
        assertTrue(event.summary().contains("NPC social state changed"));
        assertTrue(event.summary().contains("trust +4"));
        assertTrue(event.summary().contains("respect -2"));
        assertTrue(event.summary().contains("fear +1"));
        assertTrue(event.summary().contains("affinity +3"));

        NpcSocialMutationEvidence evidence = event.npcSocialMutation();
        assertEquals(mutationId, evidence.mutationId());
        assertEquals(target, evidence.targetNpcId());
        assertEquals(cause, evidence.causeEventId());
        assertEquals(900L, evidence.causeGameTime());
        assertEquals(mutation.boundedRequestedDelta(), evidence.boundedRequestedDelta());
        assertEquals(mutation.appliedDelta(), evidence.appliedDelta());
        assertEquals(mutation.before(), evidence.before());
        assertEquals(mutation.after(), evidence.after());
    }

    @Test
    void nonAppliedOrMalformedOutcomesProduceNoAuditEvent() {
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID cause = UUID.randomUUID();
        UUID mutationId = UUID.randomUUID();

        for (NpcSocialCausalMutation.Status status : new NpcSocialCausalMutation.Status[]{
                NpcSocialCausalMutation.Status.REPLAYED,
                NpcSocialCausalMutation.Status.NO_CHANGE,
                NpcSocialCausalMutation.Status.CAPACITY_REACHED,
                NpcSocialCausalMutation.Status.STALE_CAUSE,
                NpcSocialCausalMutation.Status.CONFLICTING_CAUSE,
                NpcSocialCausalMutation.Status.INVALID_PAIR,
                NpcSocialCausalMutation.Status.FRONTIER_CORRUPT
        }) {
            Optional<MemoryEvent> event = NpcSocialMutationMemoryAdapter.toMemoryEvent(
                    source,
                    mutation(
                            status,
                            mutationId,
                            source,
                            target,
                            cause,
                            1000L,
                            NpcSocialDelta.NONE,
                            NpcSocialDelta.NONE,
                            NpcSocialState.NEUTRAL,
                            NpcSocialState.NEUTRAL
                    ),
                    2000L
            );
            assertTrue(event.isEmpty(), status + " must not append a new social audit event");
        }

        assertTrue(NpcSocialMutationMemoryAdapter.toMemoryEvent(null, null, 0L).isEmpty());
    }

    private static NpcSocialCausalMutation mutation(
            NpcSocialCausalMutation.Status status,
            UUID mutationId,
            UUID source,
            UUID target,
            UUID cause,
            long gameTime,
            NpcSocialDelta boundedRequest,
            NpcSocialDelta applied,
            NpcSocialState before,
            NpcSocialState after
    ) {
        return new NpcSocialCausalMutation(
                status,
                mutationId,
                source,
                target,
                cause,
                gameTime,
                boundedRequest,
                applied,
                before,
                after
        );
    }
}
