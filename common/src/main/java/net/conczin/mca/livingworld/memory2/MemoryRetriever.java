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
        return rankCandidates(store.getRecent(query.npcId(), query.candidateLimit()), query);
    }

    static List<RankedMemory> rankCandidates(List<MemoryEvent> candidates, MemoryQuery query) {
        return rankCandidates(candidates, query, "");
    }

    static List<RankedMemory> rankCandidates(
            List<MemoryEvent> candidates,
            MemoryQuery query,
            String currentMessage
    ) {
        if (candidates == null || candidates.isEmpty() || query == null) return List.of();
        return candidates.stream()
                .map(event -> rank(event, query, currentMessage))
                .sorted(RANKING)
                .limit(query.maxResults())
                .toList();
    }

    static RankedMemory rank(MemoryEvent event, MemoryQuery query) {
        return rank(event, query, "");
    }

    static RankedMemory rank(MemoryEvent event, MemoryQuery query, String currentMessage) {
        int relevance = relevanceScore(event, query, currentMessage);
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
        return relevanceScore(event, query, "");
    }

    static int relevanceScore(MemoryEvent event, MemoryQuery query, String currentMessage) {
        if (MemoryLexicalRelevance.hasUsefulQuery(currentMessage)) {
            int dimensions = 1;
            int score = MemoryLexicalRelevance.score(currentMessage, event.summary());

            // Query-aware Memory 2.0 context already applies NPC/player eligibility before
            // candidate allocation. Treat that boundary as eligibility, not as a positive
            // relevance dimension: otherwise every eligible fresh dialogue receives a
            // structural relevance bonus that can starve an older lexical match at rank-to-6.
            if (!query.preferredTypes().isEmpty()) {
                dimensions++;
                if (query.preferredTypes().contains(event.type())) score += 100;
            }

            return score / dimensions;
        }

        int dimensions = 0;
        int score = 0;

        if (!query.participants().isEmpty()) {
            dimensions++;
            boolean npcGlobal = event.participants().stream()
                    .noneMatch(id -> !event.ownerNpcId().equals(id));
            boolean matches = npcGlobal
                    || event.participants().stream().anyMatch(query.participants()::contains);
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
