package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Loads a hard-bounded deterministic Memory 2.0 context set for one NPC turn. */
public final class Memory2ContextProvider {
    static final int CANDIDATE_LIMIT = 32;
    static final int MAX_RESULTS = 6;
    static final long RECENCY_HORIZON_TICKS = 168_000L;
    static final int QUERY_RELEVANT_LIMIT = 8;
    static final int QUERY_SCAN_LIMIT = 256;

    private static final Comparator<MemoryEvent> NEWEST_FIRST = Comparator
            .comparingLong(MemoryEvent::gameTime).reversed()
            .thenComparing(Comparator.comparingLong(MemoryEvent::createdAtEpochMillis).reversed())
            .thenComparing(event -> event.id().toString(), Comparator.reverseOrder());

    private Memory2ContextProvider() {
    }

    public static List<String> load(Path worldRoot, UUID npcId, UUID playerId, long gameTime) {
        return load(worldRoot, npcId, playerId, gameTime, "");
    }

    public static List<String> load(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            String currentMessage
    ) {
        if (worldRoot == null || npcId == null) return List.of();
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
        List<MemoryEvent> baseline = LongHorizonCandidateSelector.select(
                eligible,
                CANDIDATE_LIMIT,
                NEWEST_FIRST,
                durableFirst(gameTime),
                MemoryEvent::id
        );
        List<MemoryEvent> candidates = mergeQueryRelevantCandidates(eligible, baseline, currentMessage);
        List<RankedMemory> ranked = MemoryRetriever.rankCandidates(candidates, query, currentMessage);
        return MemoryContextFormatter.format(ranked);
    }

    private static List<MemoryEvent> mergeQueryRelevantCandidates(
            List<MemoryEvent> eligible,
            List<MemoryEvent> baseline,
            String currentMessage
    ) {
        if (!MemoryLexicalRelevance.hasUsefulQuery(currentMessage)) return baseline;

        Comparator<MemoryEvent> queryFirst = Comparator
                .comparingInt((MemoryEvent event) -> MemoryLexicalRelevance.score(currentMessage, event.summary()))
                .reversed()
                .thenComparing(NEWEST_FIRST);
        List<MemoryEvent> queryRelevant = eligible.stream()
                .limit(QUERY_SCAN_LIMIT)
                .filter(event -> MemoryLexicalRelevance.score(currentMessage, event.summary()) > 0)
                .sorted(queryFirst)
                .limit(QUERY_RELEVANT_LIMIT)
                .toList();

        if (queryRelevant.isEmpty()) return baseline;

        ArrayList<MemoryEvent> merged = new ArrayList<>(CANDIDATE_LIMIT);
        Set<UUID> seen = new HashSet<>();
        appendUniqueBounded(merged, seen, queryRelevant);
        appendUniqueBounded(merged, seen, baseline);
        return List.copyOf(merged);
    }

    private static void appendUniqueBounded(
            List<MemoryEvent> output,
            Set<UUID> seen,
            List<MemoryEvent> source
    ) {
        for (MemoryEvent event : source) {
            if (output.size() >= CANDIDATE_LIMIT) return;
            if (seen.add(event.id())) output.add(event);
        }
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
}
