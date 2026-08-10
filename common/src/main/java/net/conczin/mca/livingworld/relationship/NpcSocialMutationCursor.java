package net.conczin.mca.livingworld.relationship;

import java.util.Objects;
import java.util.UUID;

/** Latest atomic causal replay frontier retained for one source NPC. */
public record NpcSocialMutationCursor(
        UUID mutationId,
        UUID sourceNpcId,
        UUID targetNpcId,
        UUID causeEventId,
        long causeGameTime,
        NpcSocialDelta boundedRequestedDelta,
        NpcSocialDelta appliedDelta,
        NpcSocialState before,
        NpcSocialState after,
        Outcome outcome
) {
    public NpcSocialMutationCursor {
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
        sourceNpcId = Objects.requireNonNull(sourceNpcId, "sourceNpcId");
        targetNpcId = Objects.requireNonNull(targetNpcId, "targetNpcId");
        causeEventId = Objects.requireNonNull(causeEventId, "causeEventId");
        if (sourceNpcId.equals(targetNpcId)) throw new IllegalArgumentException("self social mutation is invalid");
        causeGameTime = Math.max(0L, causeGameTime);
        boundedRequestedDelta = Objects.requireNonNull(boundedRequestedDelta, "boundedRequestedDelta")
                .sanitized(NpcSocialState.MAX_VALUE);
        appliedDelta = Objects.requireNonNull(appliedDelta, "appliedDelta")
                .sanitized(NpcSocialState.MAX_VALUE);
        before = Objects.requireNonNull(before, "before");
        after = Objects.requireNonNull(after, "after");
        outcome = Objects.requireNonNull(outcome, "outcome");
    }

    public enum Outcome {
        APPLIED,
        NO_CHANGE,
        CAPACITY_REACHED
    }
}
