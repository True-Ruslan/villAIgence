package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Pure construction of canonical bounded NPC-to-NPC provenance paths. */
final class KnowledgeTransferProvenanceFactory {
    private KnowledgeTransferProvenanceFactory() {
    }

    static Optional<KnowledgeTransferProvenance> firstHop(
            SemanticMemoryEntry source,
            UUID listenerNpcId,
            UUID evidenceEventId,
            long authoritativeGameTime
    ) {
        if (source == null
                || listenerNpcId == null
                || evidenceEventId == null
                || source.ownerNpcId().equals(listenerNpcId)) {
            return Optional.empty();
        }
        boolean allowedOrigin = (source.kind() == SemanticMemoryEntry.Kind.FACT
                && source.provenance() == MemoryEvent.Provenance.SYSTEM_OBSERVED)
                || (source.kind() == SemanticMemoryEntry.Kind.BELIEF
                && (source.provenance() == MemoryEvent.Provenance.PLAYER_TOLD
                || source.provenance() == MemoryEvent.Provenance.INFERRED));
        if (!allowedOrigin) return Optional.empty();

        String statement = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(source.statement());
        if (statement.isBlank()) return Optional.empty();
        long safeGameTime = Math.max(0L, authoritativeGameTime);
        UUID expectedId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                source.ownerNpcId(), listenerNpcId, source.id(), safeGameTime);
        if (!evidenceEventId.equals(expectedId)) return Optional.empty();

        KnowledgeTransferProvenance provenance = new KnowledgeTransferProvenance(
                new KnowledgeTransferProvenance.Origin(
                        source.ownerNpcId(),
                        source.id(),
                        source.kind(),
                        source.provenance(),
                        statement,
                        KnowledgeTransferProvenancePolicy.canonicalIds(source.relatedEntities())
                ),
                List.of(new KnowledgeTransferProvenance.Hop(
                        source.ownerNpcId(),
                        listenerNpcId,
                        source.id(),
                        evidenceEventId,
                        safeGameTime
                ))
        );
        return KnowledgeTransferProvenancePolicy.valid(provenance)
                ? Optional.of(provenance)
                : Optional.empty();
    }

    static Optional<KnowledgeTransferProvenance> appendHop(
            KnowledgeTransferProvenance current,
            SemanticMemoryEntry speakerSource,
            UUID listenerNpcId,
            UUID evidenceEventId,
            long authoritativeGameTime
    ) {
        if (!KnowledgeTransferProvenancePolicy.valid(current)
                || speakerSource == null
                || speakerSource.kind() != SemanticMemoryEntry.Kind.BELIEF
                || speakerSource.provenance() != MemoryEvent.Provenance.NPC_TOLD
                || listenerNpcId == null
                || evidenceEventId == null
                || speakerSource.ownerNpcId().equals(listenerNpcId)
                || !KnowledgeTransferProvenancePolicy.contentMatchesOrigin(current, speakerSource)
                || !current.hops().getLast().listenerNpcId().equals(speakerSource.ownerNpcId())
                || KnowledgeTransferProvenancePolicy.wouldCycle(current, listenerNpcId)
                || KnowledgeTransferProvenancePolicy.atHopLimit(current)) {
            return Optional.empty();
        }

        long safeGameTime = Math.max(0L, authoritativeGameTime);
        UUID expectedId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                speakerSource.ownerNpcId(), listenerNpcId, speakerSource.id(), safeGameTime);
        if (!evidenceEventId.equals(expectedId)) return Optional.empty();

        List<KnowledgeTransferProvenance.Hop> hops = new ArrayList<>(current.hops());
        hops.add(new KnowledgeTransferProvenance.Hop(
                speakerSource.ownerNpcId(),
                listenerNpcId,
                speakerSource.id(),
                evidenceEventId,
                safeGameTime
        ));
        KnowledgeTransferProvenance result = new KnowledgeTransferProvenance(
                current.origin(),
                List.copyOf(hops)
        );
        return KnowledgeTransferProvenancePolicy.valid(result)
                ? Optional.of(result)
                : Optional.empty();
    }
}
