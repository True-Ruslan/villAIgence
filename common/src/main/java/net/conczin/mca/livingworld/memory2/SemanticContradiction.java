package net.conczin.mca.livingworld.memory2;

import java.util.List;
import java.util.UUID;

/** Immutable structured process evidence linking two distinct logical Semantic Memory claims. */
public record SemanticContradiction(
        ClaimSnapshot first,
        ClaimSnapshot second
) {
    public SemanticContradiction {
        if (first == null || second == null) {
            throw new IllegalArgumentException("both contradiction claims are required");
        }
        if (first.logicalClaimId().equals(second.logicalClaimId())) {
            throw new IllegalArgumentException("contradiction claims must be distinct");
        }
        if (first.logicalClaimId().toString().compareTo(second.logicalClaimId().toString()) > 0) {
            ClaimSnapshot swap = first;
            first = second;
            second = swap;
        }
    }

    public record ClaimSnapshot(
            UUID logicalClaimId,
            UUID detectedSemanticEntryId,
            SemanticMemoryEntry.Kind kind,
            MemoryEvent.Provenance provenance,
            List<UUID> relatedEntities
    ) {
        public ClaimSnapshot {
            if (logicalClaimId == null) throw new IllegalArgumentException("logicalClaimId is required");
            if (detectedSemanticEntryId == null) {
                throw new IllegalArgumentException("detectedSemanticEntryId is required");
            }
            if (kind == null) throw new IllegalArgumentException("kind is required");
            if (provenance == null) throw new IllegalArgumentException("provenance is required");
            relatedEntities = SemanticMemoryIdentity.canonicalIds(relatedEntities);
        }
    }
}
