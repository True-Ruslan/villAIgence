package net.conczin.mca.livingworld.memory2;

/**
 * Derived server-owned process fallibility for one retained NPC_TOLD rumor.
 * This is not a truth probability and never changes FACT/BELIEF authority.
 */
record RumorFallibilityState(
        SourcePath sourcePath,
        int sourceDistanceHops,
        int transformationsUsed
) {
    RumorFallibilityState {
        if (sourcePath == null) throw new IllegalArgumentException("sourcePath is required");
        if (transformationsUsed != 0) {
            throw new IllegalArgumentException("wording transformation is not supported in this slice");
        }
        if (sourcePath == SourcePath.RESOLVED) {
            if (sourceDistanceHops < 1 || sourceDistanceHops > KnowledgeTransferProvenancePolicy.MAX_HOPS) {
                throw new IllegalArgumentException("resolved source distance must match bounded provenance depth");
            }
        } else if (sourceDistanceHops != 0) {
            throw new IllegalArgumentException("unresolved source path cannot invent source distance");
        }
    }

    static RumorFallibilityState unresolved() {
        return new RumorFallibilityState(SourcePath.UNRESOLVED, 0, 0);
    }

    enum SourcePath {
        RESOLVED,
        UNRESOLVED
    }
}
