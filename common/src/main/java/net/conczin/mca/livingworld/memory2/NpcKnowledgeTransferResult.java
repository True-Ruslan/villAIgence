package net.conczin.mca.livingworld.memory2;

import java.util.UUID;

/** Immutable result of one server-owned NPC-to-NPC knowledge-transfer attempt. */
public record NpcKnowledgeTransferResult(
        Status status,
        UUID evidenceEventId,
        UUID semanticEntryId
) {
    public NpcKnowledgeTransferResult {
        if (status == null) throw new IllegalArgumentException("status is required");
    }

    public enum Status {
        ADMITTED,
        REJECTED,
        SOURCE_NOT_RETAINED,
        BELIEF_NOT_RETAINED
    }
}
