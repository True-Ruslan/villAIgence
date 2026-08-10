package net.conczin.mca.livingworld.memory2;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Resolves the exact player whose trust may affect a retained player-origin BELIEF. */
final class SocialEpistemicSourceResolver {
    private SocialEpistemicSourceResolver() {
    }

    static Optional<UUID> resolvePlayer(
            SemanticMemoryStore semanticStore,
            MemoryEventStore eventStore,
            SemanticMemoryEntry entry
    ) {
        if (semanticStore == null
                || eventStore == null
                || entry == null
                || entry.kind() != SemanticMemoryEntry.Kind.BELIEF
                || entry.provenance() == null) {
            return Optional.empty();
        }

        return switch (entry.provenance()) {
            case PLAYER_TOLD -> resolveDirectPlayer(eventStore, entry);
            case NPC_TOLD -> resolveTransferredPlayer(semanticStore, eventStore, entry);
            case INFERRED, SYSTEM_OBSERVED -> Optional.empty();
        };
    }

    private static Optional<UUID> resolveDirectPlayer(
            MemoryEventStore eventStore,
            SemanticMemoryEntry entry
    ) {
        if (entry.sourceEventIds() == null || entry.sourceEventIds().isEmpty()) {
            return Optional.empty();
        }

        UUID resolvedPlayer = null;
        for (UUID sourceEventId : entry.sourceEventIds()) {
            Optional<MemoryEvent> source = eventStore.findById(entry.ownerNpcId(), sourceEventId);
            if (source.isEmpty()) return Optional.empty();

            Optional<UUID> player = playerFromDirectEvidence(source.get(), entry.ownerNpcId());
            if (player.isEmpty()) return Optional.empty();
            if (resolvedPlayer != null && !resolvedPlayer.equals(player.get())) {
                return Optional.empty();
            }
            resolvedPlayer = player.get();
        }
        return Optional.ofNullable(resolvedPlayer);
    }

    private static Optional<UUID> resolveTransferredPlayer(
            SemanticMemoryStore semanticStore,
            MemoryEventStore eventStore,
            SemanticMemoryEntry entry
    ) {
        Optional<KnowledgeTransferProvenanceResolver.ResolvedSource> resolved =
                KnowledgeTransferProvenanceResolver.resolve(eventStore, entry);
        if (resolved.isEmpty()) return Optional.empty();

        KnowledgeTransferProvenance provenance = resolved.get().provenance();
        KnowledgeTransferProvenance.Origin origin = provenance.origin();
        if (origin == null
                || origin.originKind() != SemanticMemoryEntry.Kind.BELIEF
                || origin.originProvenance() != MemoryEvent.Provenance.PLAYER_TOLD) {
            return Optional.empty();
        }

        Optional<SemanticMemoryEntry> originEntry = semanticStore.findById(
                origin.originNpcId(),
                origin.originSemanticEntryId()
        );
        if (originEntry.isEmpty()
                || !KnowledgeTransferProvenancePolicy.originMatchesSource(provenance, originEntry.get())) {
            return Optional.empty();
        }
        return resolveDirectPlayer(eventStore, originEntry.get());
    }

    private static Optional<UUID> playerFromDirectEvidence(MemoryEvent event, UUID ownerNpcId) {
        if (event == null
                || ownerNpcId == null
                || !ownerNpcId.equals(event.ownerNpcId())
                || event.type() != MemoryEvent.Type.DIALOGUE
                || event.provenance() != MemoryEvent.Provenance.PLAYER_TOLD
                || event.dialogue() == null
                || event.participants() == null
                || !event.participants().contains(ownerNpcId)) {
            return Optional.empty();
        }

        Set<UUID> nonOwnerParticipants = new LinkedHashSet<>();
        for (UUID participant : event.participants()) {
            if (participant != null && !ownerNpcId.equals(participant)) {
                nonOwnerParticipants.add(participant);
            }
        }
        return nonOwnerParticipants.size() == 1
                ? Optional.of(nonOwnerParticipants.iterator().next())
                : Optional.empty();
    }
}
