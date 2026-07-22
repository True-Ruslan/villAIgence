package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Loads a hard-bounded deterministic semantic-memory context set for one NPC turn. */
public final class SemanticMemoryContextProvider {
    static final int CANDIDATE_LIMIT = 32;
    static final int MAX_RESULTS = 6;
    static final long RECENCY_HORIZON_TICKS = 168_000L;

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
        List<RankedSemanticMemory> ranked = SemanticMemoryRetriever.retrieve(SemanticMemoryStore.forWorld(worldRoot), query);
        return SemanticMemoryContextFormatter.format(ranked);
    }
}
