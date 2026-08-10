package net.conczin.mca.livingworld.memory2;

import java.util.List;
import java.util.UUID;

/** Selects a deterministic bounded set of same-scope Semantic claims for contradiction classification. */
final class SemanticContradictionCandidateSelector {
    static final int MAX_CANDIDATES_PER_ADMISSION = 16;
    static final int MAX_COMPARISONS_PER_ADMISSION = 8;

    private SemanticContradictionCandidateSelector() {
    }

    static List<SemanticMemoryEntry> select(
            SemanticMemoryStore store,
            SemanticMemoryEntry subject
    ) {
        if (store == null || subject == null) return List.of();

        UUID ownerNpcId = subject.ownerNpcId();
        UUID logicalClaimId = SemanticMemoryIdentity.logicalClaimId(subject);
        List<UUID> canonicalScope = SemanticMemoryIdentity.canonicalIds(subject.relatedEntities());
        String canonicalStatement = SemanticMemoryIdentity.canonicalStatement(subject.statement());
        if (canonicalStatement.isBlank()) return List.of();

        return store.getRecentMatching(
                ownerNpcId,
                MAX_CANDIDATES_PER_ADMISSION,
                candidate -> candidate != null
                        && ownerNpcId.equals(candidate.ownerNpcId())
                        && canonicalScope.equals(SemanticMemoryIdentity.canonicalIds(candidate.relatedEntities()))
                        && !logicalClaimId.equals(SemanticMemoryIdentity.logicalClaimId(candidate))
                        && !canonicalStatement.equals(SemanticMemoryIdentity.canonicalStatement(candidate.statement()))
                        && !SemanticMemoryIdentity.canonicalStatement(candidate.statement()).isBlank()
        );
    }
}
