package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Read-only deterministic selection of one retained direct provenance branch for an NPC_TOLD BELIEF. */
final class KnowledgeTransferProvenanceResolver {
    private static final Comparator<MemoryEvent> BRANCH_ORDER = Comparator
            .comparingLong(MemoryEvent::gameTime)
            .reversed()
            .thenComparing(event -> event.id().toString());

    private KnowledgeTransferProvenanceResolver() {
    }

    static Optional<ResolvedSource> resolve(
            MemoryEventStore eventStore,
            SemanticMemoryEntry speakerSource
    ) {
        if (eventStore == null
                || speakerSource == null
                || speakerSource.kind() != SemanticMemoryEntry.Kind.BELIEF
                || speakerSource.provenance() != MemoryEvent.Provenance.NPC_TOLD
                || speakerSource.sourceEventIds().isEmpty()) {
            return Optional.empty();
        }

        List<ResolvedSource> candidates = new ArrayList<>();
        for (var sourceEventId : speakerSource.sourceEventIds()) {
            eventStore.findById(speakerSource.ownerNpcId(), sourceEventId)
                    .flatMap(event -> resolveCandidate(event, speakerSource))
                    .ifPresent(candidates::add);
        }
        return candidates.stream()
                .sorted(Comparator.comparing(ResolvedSource::evidence, BRANCH_ORDER))
                .findFirst();
    }

    private static Optional<ResolvedSource> resolveCandidate(
            MemoryEvent evidence,
            SemanticMemoryEntry speakerSource
    ) {
        KnowledgeTransferProvenance provenance = evidence.knowledgeTransferProvenance();
        if (!KnowledgeTransferProvenancePolicy.directEvidenceMatches(
                provenance,
                evidence,
                speakerSource
        )) {
            return Optional.empty();
        }

        KnowledgeTransferProvenance.Hop lastHop = provenance.hops().getLast();
        if (!NpcKnowledgeTransferPolicy.validEvidence(
                evidence,
                lastHop.speakerNpcId(),
                lastHop.listenerNpcId(),
                lastHop.speakerSemanticEntryId(),
                lastHop.gameTime(),
                provenance.origin().statement(),
                provenance
        )) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedSource(evidence, provenance));
    }

    record ResolvedSource(
            MemoryEvent evidence,
            KnowledgeTransferProvenance provenance
    ) {
    }
}
