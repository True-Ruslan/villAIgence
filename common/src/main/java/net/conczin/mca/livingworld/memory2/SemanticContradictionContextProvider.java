package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Loads only currently resolvable, player-eligible Semantic disagreements for prompt capture. */
public final class SemanticContradictionContextProvider {
    public static final int MAX_RESULTS = 4;

    private SemanticContradictionContextProvider() {
    }

    public static List<String> load(Path worldRoot, UUID npcId, UUID playerId) {
        if (worldRoot == null || npcId == null || playerId == null) return List.of();
        return SemanticContradictionContextFormatter.format(
                SemanticContradictionHistory.load(worldRoot, npcId, playerId, MAX_RESULTS)
        );
    }
}
