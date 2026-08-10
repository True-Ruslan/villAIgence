package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;

import java.util.Optional;

/** Combines exact retained source evidence with current server-owned relationship state. */
final class SocialEpistemicResolver {
    private SocialEpistemicResolver() {
    }

    static Optional<SocialEpistemicState> resolve(
            SemanticMemoryStore semanticStore,
            MemoryEventStore eventStore,
            LivingWorldRelationshipStore relationshipStore,
            SemanticMemoryEntry entry
    ) {
        if (semanticStore == null || eventStore == null || relationshipStore == null || entry == null) {
            return Optional.empty();
        }
        return SocialEpistemicSourceResolver.resolvePlayer(semanticStore, eventStore, entry)
                .flatMap(sourcePlayerId -> SocialEpistemicPolicy.derive(
                        entry,
                        sourcePlayerId,
                        relationshipStore.get(entry.ownerNpcId(), sourcePlayerId)
                ));
    }
}
