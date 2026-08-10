package net.conczin.mca.livingworld.relationship;

import java.util.UUID;

/** Exact server-owned outcome of one directed NPC social-graph mutation attempt. */
public record NpcSocialGraphMutation(
        Status status,
        UUID sourceNpcId,
        UUID targetNpcId,
        NpcSocialState before,
        NpcSocialState after
) {
    public NpcSocialGraphMutation {
        if (status == null) throw new IllegalArgumentException("status is required");
        before = before == null ? NpcSocialState.NEUTRAL : before;
        after = after == null ? before : after;
    }

    public boolean changed() {
        return status == Status.APPLIED && !before.equals(after);
    }

    public enum Status {
        APPLIED,
        NO_CHANGE,
        INVALID_PAIR,
        CAPACITY_REACHED
    }
}
