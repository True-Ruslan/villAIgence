package net.conczin.mca.livingworld.memory2;

/** Inspectable deterministic ranking result for one Memory 2.0 event. */
public record RankedMemory(
        MemoryEvent event,
        int totalScore,
        int relevanceScore,
        int recencyScore,
        int importanceScore,
        int confidenceScore
) {
    public RankedMemory {
        if (event == null) throw new IllegalArgumentException("event is required");
        totalScore = clamp(totalScore);
        relevanceScore = clamp(relevanceScore);
        recencyScore = clamp(recencyScore);
        importanceScore = clamp(importanceScore);
        confidenceScore = clamp(confidenceScore);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
