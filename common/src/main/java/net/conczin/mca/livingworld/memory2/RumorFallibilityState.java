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
    static final int UNKNOWN_TRANSFORMATIONS = -1;

    RumorFallibilityState {
        if (sourcePath == null) throw new IllegalArgumentException("sourcePath is required");
        if (sourcePath == SourcePath.RESOLVED) {
            if (sourceDistanceHops < 1 || sourceDistanceHops > KnowledgeTransferProvenancePolicy.MAX_HOPS) {
                throw new IllegalArgumentException("resolved source distance must match bounded provenance depth");
            }
            if (transformationsUsed < 0
                    || transformationsUsed > KnowledgeTransferTransformationPolicy.MAX_TRANSFORMATIONS) {
                throw new IllegalArgumentException("resolved transformation count must match bounded evidence");
            }
        } else {
            if (sourceDistanceHops != 0) {
                throw new IllegalArgumentException("unresolved source path cannot invent source distance");
            }
            if (transformationsUsed != UNKNOWN_TRANSFORMATIONS) {
                throw new IllegalArgumentException("unresolved source path cannot invent transformation count");
            }
        }
    }

    static RumorFallibilityState unresolved() {
        return new RumorFallibilityState(
                SourcePath.UNRESOLVED,
                0,
                UNKNOWN_TRANSFORMATIONS
        );
    }

    enum SourcePath {
        RESOLVED,
        UNRESOLVED
    }
}
