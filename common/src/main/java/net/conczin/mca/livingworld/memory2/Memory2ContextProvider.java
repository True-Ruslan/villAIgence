package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Loads a hard-bounded deterministic Memory 2.0 context set for one NPC turn. */
public final class Memory2ContextProvider {
    static final int CANDIDATE_LIMIT = 32;
    static final int MAX_RESULTS = 6;
    static final long RECENCY_HORIZON_TICKS = 168_000L;
    static final int QUERY_CANDIDATE_QUOTA = CANDIDATE_LIMIT / 4;

    private static final Comparator<MemoryEvent> NEWEST_FIRST = Comparator
            .comparingLong(MemoryEvent::gameTime).reversed()
            .thenComparing(Comparator.comparingLong(MemoryEvent::createdAtEpochMillis).reversed())
            .thenComparing(event -> event.id().toString(), Comparator.reverseOrder());

    private Memory2ContextProvider() {
    }

    public static List<String> load(Path worldRoot, UUID npcId, UUID playerId, long gameTime) {
        return load(worldRoot, npcId, playerId, gameTime, MemoryRecallQueryContext.current());
    }

    public static List<String> load(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            String queryText
    ) {
        if (worldRoot == null || npcId == null) return List.of();
        String boundedQuery = MemoryQueryTextMatcher.boundQuery(queryText);
        Set<UUID> participants = playerId == null ? Set.of() : Set.of(playerId);
        MemoryQuery query = new MemoryQuery(
                npcId,
                participants,
                Set.of(),
                gameTime,
                RECENCY_HORIZON_TICKS,
                CANDIDATE_LIMIT,
                MAX_RESULTS
        );
        MemoryEventStore store = MemoryEventStore.forWorld(worldRoot);
        List<MemoryEvent> eligible = store.getRecentMatching(
                npcId,
                Integer.MAX_VALUE,
                event -> event.type() != MemoryEvent.Type.SEMANTIC_CONTRADICTION
                        && event.type() != MemoryEvent.Type.NPC_SOCIAL_CHANGE
                        && PlayerScopedMemoryEligibility.episodic(event, npcId, playerId)
        );
        List<MemoryEvent> candidates = selectCandidates(eligible, gameTime, boundedQuery);
        List<RankedMemory> ranked = boundedQuery.isBlank()
                ? MemoryRetriever.rankCandidates(candidates, query)
                : rankQueryAware(candidates, query, boundedQuery);
        return MemoryContextFormatter.format(ranked);
    }

    private static List<MemoryEvent> selectCandidates(
            List<MemoryEvent> eligible,
            long gameTime,
            String queryText
    ) {
        List<MemoryEvent> baseline = LongHorizonCandidateSelector.select(
                eligible,
                CANDIDATE_LIMIT,
                NEWEST_FIRST,
                durableFirst(gameTime),
                MemoryEvent::id
        );
        if (queryText.isBlank() || eligible == null || eligible.isEmpty()) return baseline;

        Comparator<QueryCandidate> queryFirst = Comparator
                .comparingInt(QueryCandidate::textScore).reversed()
                .thenComparing((left, right) -> durableFirst(gameTime).compare(left.event(), right.event()));

        List<QueryCandidate> relevant = eligible.stream()
                .map(event -> new QueryCandidate(
                        event,
                        MemoryQueryTextMatcher.score(queryText, event.summary())
                ))
                .filter(candidate -> candidate.textScore() > 0)
                .sorted(queryFirst)
                .limit(QUERY_CANDIDATE_QUOTA)
                .toList();
        if (relevant.isEmpty()) return baseline;

        LinkedHashMap<UUID, MemoryEvent> selected = new LinkedHashMap<>();
        for (QueryCandidate candidate : relevant) {
            selected.putIfAbsent(candidate.event().id(), candidate.event());
        }
        for (MemoryEvent candidate : baseline) {
            if (selected.size() >= CANDIDATE_LIMIT) break;
            selected.putIfAbsent(candidate.id(), candidate);
        }
        return List.copyOf(selected.values());
    }

    private static List<RankedMemory> rankQueryAware(
            List<MemoryEvent> candidates,
            MemoryQuery query,
            String queryText
    ) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        return candidates.stream()
                .map(event -> {
                    RankedMemory ranked = MemoryRetriever.rank(event, query);
                    int textScore = MemoryQueryTextMatcher.score(queryText, event.summary());
                    int blendedScore = (ranked.totalScore() * 60 + textScore * 40) / 100;
                    return new QueryRankedMemory(ranked, textScore, blendedScore);
                })
                .sorted(Comparator
                        .comparingInt(QueryRankedMemory::blendedScore).reversed()
                        .thenComparing(Comparator.comparingInt(QueryRankedMemory::textScore).reversed())
                        .thenComparing(Comparator.comparingInt(
                                value -> value.ranked().totalScore()).reversed())
                        .thenComparing(Comparator.comparingLong(
                                (QueryRankedMemory value) -> value.ranked().event().gameTime()).reversed())
                        .thenComparing(Comparator.comparingLong(
                                (QueryRankedMemory value) -> value.ranked().event().createdAtEpochMillis()).reversed())
                        .thenComparing(value -> value.ranked().event().id().toString()))
                .limit(query.maxResults())
                .map(QueryRankedMemory::ranked)
                .toList();
    }

    private static Comparator<MemoryEvent> durableFirst(long gameTime) {
        return Comparator
                .comparingLong((MemoryEvent event) ->
                        MemoryEventRetentionPolicy.effectiveRetentionScore(event, gameTime))
                .reversed()
                .thenComparing(Comparator.comparingInt(MemoryEvent::importance).reversed())
                .thenComparing(Comparator.comparingInt(MemoryEvent::confidence).reversed())
                .thenComparing(Comparator.comparingInt(
                        (MemoryEvent event) -> Math.abs(event.emotionalWeight())).reversed())
                .thenComparing(Comparator.comparingInt(
                        (MemoryEvent event) -> MemoryEventRetentionPolicy.typeContribution(event.type())).reversed())
                .thenComparing(Comparator.comparingInt(
                        (MemoryEvent event) -> MemoryEventRetentionPolicy.provenanceContribution(event.provenance())).reversed())
                .thenComparing(Comparator.comparingLong(MemoryEvent::gameTime).reversed())
                .thenComparing(Comparator.comparingLong(MemoryEvent::createdAtEpochMillis).reversed())
                .thenComparing(event -> event.id().toString());
    }

    private record QueryCandidate(MemoryEvent event, int textScore) {
    }

    private record QueryRankedMemory(RankedMemory ranked, int textScore, int blendedScore) {
    }
}
