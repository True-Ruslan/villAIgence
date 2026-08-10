package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;

import java.util.Optional;
import java.util.UUID;

/** Pure bounded policy for deriving current personal reliance on a player-origin BELIEF. */
final class SocialEpistemicPolicy {
    private SocialEpistemicPolicy() {
    }

    static Optional<SocialEpistemicState> derive(
            SemanticMemoryEntry entry,
            UUID sourcePlayerId,
            LivingWorldRelationshipState relationshipState
    ) {
        if (entry == null
                || sourcePlayerId == null
                || relationshipState == null
                || entry.kind() != SemanticMemoryEntry.Kind.BELIEF
                || (entry.provenance() != MemoryEvent.Provenance.PLAYER_TOLD
                && entry.provenance() != MemoryEvent.Provenance.NPC_TOLD)) {
            return Optional.empty();
        }

        int trust = relationshipState.trust();
        int trustDelta = Math.max(-10, Math.min(10, trust / 10));
        int effectiveConfidence = Math.max(0, Math.min(100, entry.confidence() + trustDelta));
        return Optional.of(new SocialEpistemicState(
                sourcePlayerId,
                trust,
                trustDelta,
                effectiveConfidence
        ));
    }
}
