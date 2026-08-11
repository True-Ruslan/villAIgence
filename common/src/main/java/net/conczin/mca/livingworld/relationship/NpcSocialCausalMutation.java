package net.conczin.mca.livingworld.relationship;

import java.util.UUID;

/** Exact result of one atomic causal social-graph mutation attempt. */
public record NpcSocialCausalMutation(
        Status status,
        UUID mutationId,
        UUID sourceNpcId,
        UUID targetNpcId,
        UUID causeEventId,
        long causeGameTime,
        NpcSocialDelta boundedRequestedDelta,
        NpcSocialDelta appliedDelta,
        NpcSocialState before,
        NpcSocialState after
) {
    public NpcSocialCausalMutation {
        if (status == null) throw new IllegalArgumentException("status is required");
        causeGameTime = Math.max(0L, causeGameTime);
        boundedRequestedDelta = boundedRequestedDelta == null ? NpcSocialDelta.NONE : boundedRequestedDelta;
        appliedDelta = appliedDelta == null ? NpcSocialDelta.NONE : appliedDelta;
        before = before == null ? NpcSocialState.NEUTRAL : before;
        after = after == null ? before : after;
    }

    static NpcSocialCausalMutation fromCursor(Status status, NpcSocialMutationCursor cursor) {
        return new NpcSocialCausalMutation(
                status,
                cursor.mutationId(),
                cursor.sourceNpcId(),
                cursor.targetNpcId(),
                cursor.causeEventId(),
                cursor.causeGameTime(),
                cursor.boundedRequestedDelta(),
                cursor.appliedDelta(),
                cursor.before(),
                cursor.after()
        );
    }

    public enum Status {
        APPLIED,
        NO_CHANGE,
        CAPACITY_REACHED,
        REPLAYED,
        STALE_CAUSE,
        CONFLICTING_CAUSE,
        INVALID_PAIR,
        FRONTIER_CORRUPT
    }
}
