package net.conczin.mca.livingworld.memory2;

import java.util.UUID;

/** Exact prompt-visibility policy for player-scoped Memory 2.0 data. */
public final class PlayerScopedMemoryEligibility {
    private PlayerScopedMemoryEligibility() {
    }

    public static boolean semantic(SemanticMemoryEntry entry, UUID npcId, UUID playerId) {
        if (entry == null || npcId == null || !npcId.equals(entry.ownerNpcId())) return false;
        if (entry.relatedEntities().isEmpty()) return true;
        return playerId != null && entry.relatedEntities().contains(playerId);
    }
}
