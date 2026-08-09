package net.conczin.mca.livingworld.memory2;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Pure construction of canonical server-owned semantic contradiction evidence. */
final class SemanticContradictionAdapter {
    private SemanticContradictionAdapter() {
    }

    static Optional<MemoryEvent> create(
            SemanticMemoryEntry first,
            SemanticMemoryEntry second,
            long authoritativeGameTime
    ) {
        if (first == null
                || second == null
                || first.id().equals(second.id())
                || !first.ownerNpcId().equals(second.ownerNpcId())) {
            return Optional.empty();
        }

        String firstStatement = SemanticMemoryIdentity.canonicalStatement(first.statement());
        String secondStatement = SemanticMemoryIdentity.canonicalStatement(second.statement());
        if (firstStatement.isBlank()
                || secondStatement.isBlank()
                || firstStatement.equals(secondStatement)) {
            return Optional.empty();
        }

        List<UUID> firstScope = SemanticMemoryIdentity.canonicalIds(first.relatedEntities());
        List<UUID> secondScope = SemanticMemoryIdentity.canonicalIds(second.relatedEntities());
        if (!firstScope.equals(secondScope)) return Optional.empty();

        SemanticContradiction contradiction = new SemanticContradiction(
                snapshot(first, firstScope),
                snapshot(second, secondScope)
        );
        long safeGameTime = Math.max(0L, authoritativeGameTime);
        MemoryEvent event = new MemoryEvent(
                SemanticContradictionPolicy.deterministicEventId(
                        first.ownerNpcId(), contradiction, safeGameTime),
                first.ownerNpcId(),
                MemoryEvent.Type.SEMANTIC_CONTRADICTION,
                SemanticContradictionPolicy.SUMMARY,
                List.of(first.ownerNpcId()),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                safeGameTime,
                0L,
                60,
                0,
                100,
                List.of(),
                null,
                null,
                null,
                null,
                contradiction
        );
        return SemanticContradictionPolicy.valid(event)
                ? Optional.of(event)
                : Optional.empty();
    }

    private static SemanticContradiction.ClaimSnapshot snapshot(
            SemanticMemoryEntry entry,
            List<UUID> canonicalScope
    ) {
        return new SemanticContradiction.ClaimSnapshot(
                SemanticMemoryIdentity.logicalClaimId(entry),
                entry.id(),
                entry.kind(),
                entry.provenance(),
                canonicalScope
        );
    }
}
