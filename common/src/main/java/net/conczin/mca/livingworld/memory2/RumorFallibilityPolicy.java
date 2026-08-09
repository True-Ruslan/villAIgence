package net.conczin.mca.livingworld.memory2;

import java.util.Optional;

/** Pure derivation of rumor fallibility from retained canonical transfer provenance. */
final class RumorFallibilityPolicy {
    private RumorFallibilityPolicy() {
    }

    static Optional<RumorFallibilityState> resolve(KnowledgeTransferProvenance provenance) {
        return resolve(provenance, null);
    }

    static Optional<RumorFallibilityState> resolve(
            KnowledgeTransferProvenance provenance,
            KnowledgeTransferTransformation transformation
    ) {
        if (!KnowledgeTransferProvenancePolicy.valid(provenance)) return Optional.empty();
        if (transformation != null
                && !KnowledgeTransferTransformationPolicy.valid(transformation, provenance)) {
            return Optional.empty();
        }
        int transformationsUsed = transformation == null
                ? 0
                : transformation.transformationsUsed();
        return Optional.of(new RumorFallibilityState(
                RumorFallibilityState.SourcePath.RESOLVED,
                provenance.hops().size(),
                transformationsUsed
        ));
    }
}
