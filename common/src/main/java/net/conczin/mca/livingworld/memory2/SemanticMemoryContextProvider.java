package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Loads a hard-bounded deterministic semantic-memory context set for one NPC turn. */
public final class SemanticMemoryContextProvider {
    static final int CANDIDATE_LIMIT = 32;
    static final int MAX_RESULTS = 6;
    static final long RECENCY_HORIZON_TICKS = 168_000L;

    private static final Comparator<SemanticMemoryEntry> NEWEST_FIRST = Comparator
            .comparingLong(SemanticMemoryEntry::gameTime).reversed()
            .thenComparing(Comparator.comparingLong(SemanticMemoryEntry::createdAtEpochMillis).reversed())
            .thenComparing(entry -> entry.id().toString(), Comparator.reverseOrder());

    private SemanticMemoryContextProvider() {
    }

    public static List<String> load(Path worldRoot, UUID npcId, UUID playerId, long gameTime) {
        if (worldRoot == null || npcId == null) return List.of();
        Set<UUID> relatedEntities = playerId == null ? Set.of() : Set.of(playerId);
        SemanticMemoryQuery query = new SemanticMemoryQuery(
                npcId,
                relatedEntities,
                gameTime,
                RECENCY_HORIZON_TICKS,
                CANDIDATE_LIMIT,
                MAX_RESULTS
        );
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(worldRoot);
        List<SemanticMemoryEntry> eligible = store.getRecentMatching(
                npcId,
                Integer.MAX_VALUE,
                entry -> PlayerScopedMemoryEligibility.semantic(entry, npcId, playerId)
        );
        List<SemanticMemoryEntry> candidates = LongHorizonCandidateSelector.select(
                eligible,
                CANDIDATE_LIMIT,
                NEWEST_FIRST,
                durableFirst(gameTime),
                SemanticMemoryEntry::id
        );
        List<RankedSemanticMemory> ranked = SemanticMemoryRetriever.rankCandidates(candidates, query);
        MemoryEventStore eventStore = MemoryEventStore.forWorld(worldRoot);
        return SemanticMemoryContextFormatter.format(
                ranked,
                eventStore,
                store,
                LivingWorldRelationshipStore.forWorld(worldRoot)
        );
    }

    private static Comparator<SemanticMemoryEntry> durableFirst(long gameTime) {
        return Comparator
                .comparingLong((SemanticMemoryEntry entry) ->
                        SemanticMemoryRetentionPolicy.effectiveRetentionScore(entry, gameTime))
                .reversed()
                .thenComparing(Comparator.comparingInt(SemanticMemoryEntry::importance).reversed())
                .thenComparing(Comparator.comparingInt(SemanticMemoryEntry::confidence).reversed())
                .thenComparing(Comparator.comparingInt(
                        (SemanticMemoryEntry entry) -> entry.sourceEventIds().size()).reversed())
                .thenComparing(Comparator.comparingLong(SemanticMemoryEntry::gameTime).reversed())
                .thenComparing(Comparator.comparingLong(SemanticMemoryEntry::createdAtEpochMillis).reversed())
                .thenComparing(entry -> entry.id().toString());
    }
}
