package net.conczin.mca.livingworld.memory2;

import java.util.UUID;

/** Immutable result of one server-owned semantic contradiction recording attempt. */
public record SemanticContradictionResult(
        Status status,
        UUID eventId
) {
    public SemanticContradictionResult {
        if (status == null) throw new IllegalArgumentException("status is required");
    }

    public enum Status {
        RECORDED,
        REJECTED,
        SOURCE_NOT_RETAINED,
        SCOPE_MISMATCH,
        SAME_CLAIM,
        EVENT_NOT_RETAINED
    }
}
