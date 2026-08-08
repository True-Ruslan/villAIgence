package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Loads a hard-bounded deterministic Memory 2.0 context set for one NPC turn. */
public final class Memory2ContextProvider {
    static final int CANDIDATE_LIMIT = 32;
    static final int MAX_RESULTS = 6;
    static final long RECENCY_HORIZON_TICKS = 168_000L;

    private static final Comparator<MemoryEvent> NEWEST_FIRST = Comparator
            .comparingLong(MemoryEvent::gameTime).reversed()
            .thenComparing(Comparator.comparingLong(MemoryEvent::createdAtEpochMillis).reversed())
            .thenComparing(event -> event.id().toString(), Comparator.reverseOrder());

    private Memory2ContextProvider() {
    }

    public static List<String> load(Path worldRoot, UUID npcId, UUID playerId, long gameTime) {
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
                event -> PlayerScopedMemoryEligibility.episodic(event, npcId, playerId)
        );
        List<MemoryEvent> candidates = LongHorizonCandidateSelector.select(
                eligible,
                CANDIDATE_LIMIT,
                NEWEST_FIRST,
                durableFirst(gameTime),
                MemoryEvent::id
        );
        List<RankedMemory> ranked = MemoryRetriever.rankCandidates(candidates, query);
        return MemoryContextFormatter.format(ranked);
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
