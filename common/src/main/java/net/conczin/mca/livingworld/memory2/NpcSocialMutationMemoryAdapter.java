package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialCausalMutation;
import net.conczin.mca.livingworld.relationship.NpcSocialDelta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Converts one original applied causal NPC social mutation into structured Memory 2.0 audit evidence. */
public final class NpcSocialMutationMemoryAdapter {
    private NpcSocialMutationMemoryAdapter() {
    }

    public static Optional<MemoryEvent> toMemoryEvent(
            UUID sourceNpcId,
            NpcSocialCausalMutation mutation,
            long createdAtEpochMillis
    ) {
        if (sourceNpcId == null
                || mutation == null
                || mutation.status() != NpcSocialCausalMutation.Status.APPLIED
                || mutation.mutationId() == null
                || !sourceNpcId.equals(mutation.sourceNpcId())
                || mutation.targetNpcId() == null
                || mutation.causeEventId() == null
                || mutation.boundedRequestedDelta() == null
                || mutation.appliedDelta() == null
                || mutation.appliedDelta().isZero()
                || mutation.before() == null
                || mutation.after() == null
                || mutation.before().equals(mutation.after())) {
            return Optional.empty();
        }

        NpcSocialDelta applied = mutation.appliedDelta();
        String summary = "NPC social state changed: trust " + signed(applied.trust())
                + ", respect " + signed(applied.respect())
                + ", fear " + signed(applied.fear())
                + ", affinity " + signed(applied.affinity())
                + "; now trust=" + mutation.after().trust()
                + ", respect=" + mutation.after().respect()
                + ", fear=" + mutation.after().fear()
                + ", affinity=" + mutation.after().affinity() + ".";

        NpcSocialMutationEvidence evidence = new NpcSocialMutationEvidence(
                mutation.mutationId(),
                mutation.targetNpcId(),
                mutation.causeEventId(),
                mutation.causeGameTime(),
                mutation.boundedRequestedDelta(),
                mutation.appliedDelta(),
                mutation.before(),
                mutation.after()
        );

        return Optional.of(new MemoryEvent(
                mutation.mutationId(),
                sourceNpcId,
                MemoryEvent.Type.NPC_SOCIAL_CHANGE,
                summary,
                List.of(sourceNpcId, mutation.targetNpcId()),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                mutation.causeGameTime(),
                createdAtEpochMillis,
                60,
                0,
                100,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                evidence
        ));
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }
}
