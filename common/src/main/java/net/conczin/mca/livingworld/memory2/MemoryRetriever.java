package net.conczin.mca.livingworld.memory2;

import java.util.Comparator;
import java.util.List;

/** Pure deterministic bounded retrieval policy for Memory 2.0 events. */
public final class MemoryRetriever {
    private static final Comparator<RankedMemory> RANKING = Comparator
            .comparingInt(RankedMemory::totalScore).reversed()
            .thenComparing(Comparator.comparingInt(RankedMemory::relevanceScore).reversed())
            .thenComparing(Comparator.comparingInt(RankedMemory::importanceScore).reversed())
            .thenComparing(Comparator.comparingInt(RankedMemory::recencyScore).reversed())
            .thenComparing(Comparator.comparingInt(RankedMemory::confidenceScore).reversed())
            .thenComparing(Comparator.comparingLong((RankedMemory value) -> value.event().gameTime()).reversed())
            .thenComparing(Comparator.comparingLong((RankedMemory value) -> value.event().createdAtEpochMillis()).reversed())
            .thenComparing(value -> value.event().id().toString());

    private MemoryRetriever() {
    }

    public static List<RankedMemory> retrieve(MemoryEventStore store, MemoryQuery query) {
        if (store == null || query == null) return List.of();
        return store.getRecent(query.npcId(), query.candidateLimit()).stream()
                .map(event -> rank(event, query))
                .sorted(RANKING)
                .limit(query.maxResults())
                .toList();
    }

    static RankedMemory rank(MemoryEvent event, MemoryQuery query) {
        int relevance = relevanceScore(event, query);
        int recency = recencyScore(event, query);
        int importance = event.importance();
        int confidence = event.confidence();
        int total = (
                relevance * 40
                        + importance * 25
                        + recency * 20
                        + confidence * 15
        ) / 100;
        return new RankedMemory(event, total, relevance, recency, importance, confidence);
    }

    static int relevanceScore(MemoryEvent event, MemoryQuery query) {
        int dimensions = 0;
        int score = 0;

        if (!query.participants().isEmpty()) {
            dimensions++;
            boolean matches = event.participants().stream().anyMatch(query.participants()::contains);
            if (matches) score += 100;
        }

        if (!query.preferredTypes().isEmpty()) {
            dimensions++;
            if (query.preferredTypes().contains(event.type())) score += 100;
        }

        return dimensions == 0 ? 100 : score / dimensions;
    }

    static int recencyScore(MemoryEvent event, MemoryQuery query) {
        long age = query.nowGameTime() - event.gameTime();
        if (age <= 0L) return 100;
        if (age >= query.recencyHorizonTicks()) return 0;

        double ratio = (double) age / (double) query.recencyHorizonTicks();
        return Math.max(0, Math.min(100, 100 - (int) (ratio * 100.0D)));
    }
}
