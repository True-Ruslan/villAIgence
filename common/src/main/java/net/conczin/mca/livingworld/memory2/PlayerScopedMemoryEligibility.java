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

    public static boolean episodic(MemoryEvent event, UUID npcId, UUID playerId) {
        if (event == null || npcId == null || !npcId.equals(event.ownerNpcId())) return false;
        boolean hasExternalParticipant = event.participants().stream()
                .anyMatch(id -> !npcId.equals(id));
        if (!hasExternalParticipant) return true;
        return playerId != null && event.participants().contains(playerId);
    }
}
