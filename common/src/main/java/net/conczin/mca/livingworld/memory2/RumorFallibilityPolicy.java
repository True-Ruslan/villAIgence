package net.conczin.mca.livingworld.memory2;

import java.util.Optional;

/** Pure derivation of rumor fallibility from retained canonical transfer provenance. */
final class RumorFallibilityPolicy {
    private RumorFallibilityPolicy() {
    }

    static Optional<RumorFallibilityState> resolve(KnowledgeTransferProvenance provenance) {
        if (!KnowledgeTransferProvenancePolicy.valid(provenance)) return Optional.empty();
        return Optional.of(new RumorFallibilityState(
                RumorFallibilityState.SourcePath.RESOLVED,
                provenance.hops().size(),
                0
        ));
    }
}
