package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Loads a hard-bounded deterministic Memory 2.0 context set for one NPC turn. */
public final class Memory2ContextProvider {
    static final int CANDIDATE_LIMIT = 32;
    static final int MAX_RESULTS = 6;
    static final long RECENCY_HORIZON_TICKS = 168_000L;

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
        List<RankedMemory> ranked = MemoryRetriever.retrieve(MemoryEventStore.forWorld(worldRoot), query);
        return MemoryContextFormatter.format(ranked);
    }
}
