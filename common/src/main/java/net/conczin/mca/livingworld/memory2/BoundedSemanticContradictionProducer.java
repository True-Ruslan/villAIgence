package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Produces bounded server-owned contradiction evidence for one already-retained Semantic claim.
 *
 * <p>Candidate selection and duplicate suppression happen before the hard classifier budget. The
 * existing {@link SemanticContradictionLifecycle} remains the only persistence authority.</p>
 */
final class BoundedSemanticContradictionProducer {
    private BoundedSemanticContradictionProducer() {
    }

    static ProductionResult produce(
            Path worldRoot,
            SemanticMemoryEntry retainedClaim,
            int maxEventsPerNpc
    ) {
        if (worldRoot == null || retainedClaim == null || maxEventsPerNpc <= 0) {
            return ProductionResult.empty();
        }

        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(worldRoot);
        List<SemanticMemoryEntry> candidates =
                SemanticContradictionCandidateSelector.select(semanticStore, retainedClaim);
        if (candidates.isEmpty()) {
            return new ProductionResult(0, 0, 0, List.of());
        }

        Set<RelationKey> retainedRelations = retainedRelationKeys(worldRoot, retainedClaim.ownerNpcId());
        UUID subjectLogicalId = SemanticMemoryIdentity.logicalClaimId(retainedClaim);
        int comparisons = 0;
        int oppositions = 0;
        List<UUID> recordedEventIds = new ArrayList<>();

        for (SemanticMemoryEntry candidate : candidates) {
            RelationKey pair = RelationKey.of(
                    subjectLogicalId,
                    SemanticMemoryIdentity.logicalClaimId(candidate)
            );
            if (retainedRelations.contains(pair)) continue;
            if (comparisons >= SemanticContradictionCandidateSelector.MAX_COMPARISONS_PER_ADMISSION) break;

            comparisons++;
            if (!SemanticOppositionClassifier.opposes(retainedClaim, candidate)) continue;

            oppositions++;
            SemanticContradictionResult result = SemanticContradictionLifecycle.record(
                    worldRoot,
                    retainedClaim.ownerNpcId(),
                    retainedClaim.id(),
                    candidate.id(),
                    retainedClaim.gameTime(),
                    maxEventsPerNpc
            );
            if (result.status() == SemanticContradictionResult.Status.RECORDED && result.eventId() != null) {
                recordedEventIds.add(result.eventId());
                retainedRelations.add(pair);
            }
        }

        return new ProductionResult(
                candidates.size(),
                comparisons,
                oppositions,
                recordedEventIds
        );
    }

    private static Set<RelationKey> retainedRelationKeys(Path worldRoot, UUID npcId) {
        Set<RelationKey> keys = new HashSet<>();
        List<MemoryEvent> events = MemoryEventStore.forWorld(worldRoot).getRecentMatching(
                npcId,
                Integer.MAX_VALUE,
                event -> event.type() == MemoryEvent.Type.SEMANTIC_CONTRADICTION
                        && SemanticContradictionPolicy.valid(event)
        );
        for (MemoryEvent event : events) {
            SemanticContradiction contradiction = event.semanticContradiction();
            keys.add(RelationKey.of(
                    contradiction.first().logicalClaimId(),
                    contradiction.second().logicalClaimId()
            ));
        }
        return keys;
    }

    record ProductionResult(
            int eligibleCandidates,
            int comparisons,
            int oppositions,
            List<UUID> recordedEventIds
    ) {
        ProductionResult {
            eligibleCandidates = Math.max(0, eligibleCandidates);
            comparisons = Math.max(0, comparisons);
            oppositions = Math.max(0, oppositions);
            recordedEventIds = recordedEventIds == null ? List.of() : List.copyOf(recordedEventIds);
        }

        static ProductionResult empty() {
            return new ProductionResult(0, 0, 0, List.of());
        }
    }

    private record RelationKey(UUID first, UUID second) {
        private static RelationKey of(UUID first, UUID second) {
            if (first == null || second == null) throw new IllegalArgumentException("claim ids are required");
            return first.toString().compareTo(second.toString()) <= 0
                    ? new RelationKey(first, second)
                    : new RelationKey(second, first);
        }
    }
}
