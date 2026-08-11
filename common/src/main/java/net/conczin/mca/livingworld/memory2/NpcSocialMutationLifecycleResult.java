package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialCausalMutation;

/** Exact lifecycle result for one server-authorized NPC social mutation attempt. */
public record NpcSocialMutationLifecycleResult(
        Status status,
        NpcSocialCausalMutation graphMutation,
        MemoryEvent auditEvent
) {
    public NpcSocialMutationLifecycleResult {
        if (status == null) throw new IllegalArgumentException("status is required");
    }

    public enum Status {
        APPLIED,
        APPLIED_AUDIT_NOT_RETAINED,
        NO_CHANGE,
        CAPACITY_REACHED,
        REPLAYED,
        STALE_CAUSE,
        CONFLICTING_CAUSE,
        SOURCE_NOT_RETAINED,
        INVALID_SOURCE_EVENT,
        INVALID_NPC,
        INVALID_REQUEST,
        FRONTIER_CORRUPT
    }
}
