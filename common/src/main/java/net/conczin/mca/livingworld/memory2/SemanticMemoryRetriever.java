package net.conczin.mca.livingworld.memory2;

import java.util.Comparator;
import java.util.List;

/** Pure deterministic bounded retrieval policy for semantic Memory 2.0 entries. */
public final class SemanticMemoryRetriever {
    private static final Comparator<RankedSemanticMemory> RANKING = Comparator
            .comparingInt(RankedSemanticMemory::totalScore).reversed()
            .thenComparing(Comparator.comparingInt(RankedSemanticMemory::relevanceScore).reversed())
            .thenComparing(Comparator.comparingInt(RankedSemanticMemory::importanceScore).reversed())
            .thenComparing(Comparator.comparingInt(RankedSemanticMemory::confidenceScore).reversed())
            .thenComparing(Comparator.comparingInt(RankedSemanticMemory::recencyScore).reversed())
            .thenComparing(Comparator.comparingLong((RankedSemanticMemory value) -> value.entry().gameTime()).reversed())
            .thenComparing(Comparator.comparingLong((RankedSemanticMemory value) -> value.entry().createdAtEpochMillis()).reversed())
            .thenComparing(value -> value.entry().id().toString());

    private SemanticMemoryRetriever() {
    }

    public static List<RankedSemanticMemory> retrieve(SemanticMemoryStore store, SemanticMemoryQuery query) {
        if (store == null || query == null) return List.of();
        return store.getRecent(query.npcId(), query.candidateLimit()).stream()
                .map(entry -> rank(entry, query))
                .sorted(RANKING)
                .limit(query.maxResults())
                .toList();
    }

    static RankedSemanticMemory rank(SemanticMemoryEntry entry, SemanticMemoryQuery query) {
        int relevance = relevanceScore(entry, query);
        int importance = entry.importance();
        int confidence = entry.confidence();
        int recency = recencyScore(entry, query);
        int total = (
                relevance * 40
                        + importance * 30
                        + confidence * 20
                        + recency * 10
        ) / 100;
        return new RankedSemanticMemory(entry, total, relevance, importance, confidence, recency);
    }

    static int relevanceScore(SemanticMemoryEntry entry, SemanticMemoryQuery query) {
        if (query.relatedEntities().isEmpty()) return 100;
        boolean matches = entry.relatedEntities().stream().anyMatch(query.relatedEntities()::contains);
        return matches ? 100 : 0;
    }

    static int recencyScore(SemanticMemoryEntry entry, SemanticMemoryQuery query) {
        long age = query.nowGameTime() - entry.gameTime();
        if (age <= 0L) return 100;
        if (age >= query.recencyHorizonTicks()) return 0;

        double ratio = (double) age / (double) query.recencyHorizonTicks();
        return Math.max(0, Math.min(100, 100 - (int) (ratio * 100.0D)));
    }
}
