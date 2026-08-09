package net.conczin.mca.livingworld.memory2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** Pure fail-closed integrity and identity policy for structured semantic contradiction evidence. */
final class SemanticContradictionPolicy {
    static final String SUMMARY = "Semantic contradiction recorded";
    private static final String ID_NAMESPACE = "semantic-contradiction-v1";

    private SemanticContradictionPolicy() {
    }

    static UUID deterministicEventId(
            UUID ownerNpcId,
            SemanticContradiction contradiction,
            long authoritativeGameTime
    ) {
        if (ownerNpcId == null) throw new IllegalArgumentException("ownerNpcId is required");
        if (contradiction == null) throw new IllegalArgumentException("contradiction is required");
        long safeGameTime = Math.max(0L, authoritativeGameTime);
        String canonical = ID_NAMESPACE
                + '\n' + ownerNpcId
                + '\n' + snapshotCanonical(contradiction.first())
                + '\n' + snapshotCanonical(contradiction.second())
                + '\n' + safeGameTime;
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    static boolean valid(MemoryEvent event) {
        if (event == null || event.semanticContradiction() == null) return false;
        SemanticContradiction contradiction = event.semanticContradiction();
        if (event.type() != MemoryEvent.Type.SEMANTIC_CONTRADICTION
                || event.provenance() != MemoryEvent.Provenance.SYSTEM_OBSERVED
                || !SUMMARY.equals(event.summary())
                || !List.of(event.ownerNpcId()).equals(event.participants())
                || event.createdAtEpochMillis() != 0L
                || event.importance() != 60
                || event.emotionalWeight() != 0
                || event.confidence() != 100
                || !event.relationshipReasons().isEmpty()
                || event.dialogue() != null
                || event.relationshipTransition() != null
                || event.relationshipCause() != null
                || event.knowledgeTransferProvenance() != null
                || !validSnapshot(contradiction.first())
                || !validSnapshot(contradiction.second())
                || !contradiction.first().relatedEntities().equals(contradiction.second().relatedEntities())
                || contradiction.first().logicalClaimId().toString()
                .compareTo(contradiction.second().logicalClaimId().toString()) >= 0) {
            return false;
        }
        return event.id().equals(deterministicEventId(
                event.ownerNpcId(),
                contradiction,
                event.gameTime()
        ));
    }

    private static boolean validSnapshot(SemanticContradiction.ClaimSnapshot snapshot) {
        if (snapshot == null) return false;
        if (!snapshot.relatedEntities().equals(SemanticMemoryIdentity.canonicalIds(snapshot.relatedEntities()))) {
            return false;
        }
        return switch (snapshot.kind()) {
            case FACT -> snapshot.provenance() == MemoryEvent.Provenance.SYSTEM_OBSERVED;
            case BELIEF -> snapshot.provenance() != MemoryEvent.Provenance.SYSTEM_OBSERVED;
        };
    }

    private static String snapshotCanonical(SemanticContradiction.ClaimSnapshot snapshot) {
        StringBuilder canonical = new StringBuilder()
                .append(snapshot.logicalClaimId()).append('\n')
                .append(snapshot.detectedSemanticEntryId()).append('\n')
                .append(snapshot.kind()).append('\n')
                .append(snapshot.provenance());
        for (UUID relatedEntity : snapshot.relatedEntities()) {
            canonical.append('\n').append(relatedEntity);
        }
        return canonical.toString();
    }
}
