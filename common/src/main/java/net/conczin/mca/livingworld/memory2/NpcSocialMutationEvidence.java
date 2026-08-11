package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialState;

import java.util.Objects;
import java.util.UUID;

/** Structured server-owned audit payload for one applied NPC-to-NPC social mutation. */
public record NpcSocialMutationEvidence(
        UUID mutationId,
        UUID targetNpcId,
        UUID causeEventId,
        long causeGameTime,
        NpcSocialDelta boundedRequestedDelta,
        NpcSocialDelta appliedDelta,
        NpcSocialState before,
        NpcSocialState after
) {
    public NpcSocialMutationEvidence {
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
        targetNpcId = Objects.requireNonNull(targetNpcId, "targetNpcId");
        causeEventId = Objects.requireNonNull(causeEventId, "causeEventId");
        causeGameTime = Math.max(0L, causeGameTime);
        boundedRequestedDelta = Objects.requireNonNull(boundedRequestedDelta, "boundedRequestedDelta")
                .sanitized(NpcSocialState.MAX_VALUE);
        appliedDelta = Objects.requireNonNull(appliedDelta, "appliedDelta")
                .sanitized(NpcSocialState.MAX_VALUE);
        before = Objects.requireNonNull(before, "before");
        after = Objects.requireNonNull(after, "after");

        NpcSocialDelta actual = new NpcSocialDelta(
                after.trust() - before.trust(),
                after.respect() - before.respect(),
                after.fear() - before.fear(),
                after.affinity() - before.affinity()
        );
        if (!actual.equals(appliedDelta)) {
            throw new IllegalArgumentException("appliedDelta must match exact before/after state");
        }
    }
}
