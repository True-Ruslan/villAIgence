package net.conczin.mca.livingworld.memory2;

/** Inspectable deterministic semantic-memory ranking result. */
public record RankedSemanticMemory(
        SemanticMemoryEntry entry,
        int totalScore,
        int relevanceScore,
        int importanceScore,
        int confidenceScore,
        int recencyScore
) {
}
