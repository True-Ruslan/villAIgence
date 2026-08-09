package net.conczin.mca.livingworld.memory2;

import java.util.Optional;

/** Read-only fallibility resolution for retained NPC_TOLD Semantic rumors. */
final class RumorFallibilityResolver {
    private RumorFallibilityResolver() {
    }

    static Optional<RumorFallibilityState> resolve(
            MemoryEventStore eventStore,
            SemanticMemoryEntry entry
    ) {
        if (eventStore == null
                || entry == null
                || entry.kind() != SemanticMemoryEntry.Kind.BELIEF
                || entry.provenance() != MemoryEvent.Provenance.NPC_TOLD) {
            return Optional.empty();
        }

        return Optional.of(KnowledgeTransferProvenanceResolver.resolve(eventStore, entry)
                .flatMap(resolved -> RumorFallibilityPolicy.resolve(resolved.provenance()))
                .orElseGet(RumorFallibilityState::unresolved));
    }
}
