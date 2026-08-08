package net.conczin.mca.livingworld.memory2;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fail-closed admission boundary for non-authoritative semantic BELIEF inputs.
 *
 * <p>The source event is always retained as evidence. Told claims require matching
 * DIALOGUE provenance; inferred claims may be derived from any explicit persisted
 * Memory 2.0 event. SYSTEM_OBSERVED is reserved for the FACT path.</p>
 */
public final class SemanticBeliefAdmissionPolicy {
    private SemanticBeliefAdmissionPolicy() {
    }

    public static Optional<SemanticBeliefSource> admit(
            MemoryEvent sourceEvent,
            String statement,
            List<UUID> relatedEntities,
            MemoryEvent.Provenance provenance,
            int importance,
            int confidence
    ) {
        if (sourceEvent == null || statement == null || statement.isBlank() || provenance == null) {
            return Optional.empty();
        }
        if (provenance == MemoryEvent.Provenance.SYSTEM_OBSERVED) {
            return Optional.empty();
        }
        if (!sourceSupports(provenance, sourceEvent)) {
            return Optional.empty();
        }

        return Optional.of(new SemanticBeliefSource(
                sourceEvent.ownerNpcId(),
                statement,
                relatedEntities,
                provenance,
                sourceEvent.gameTime(),
                sourceEvent.createdAtEpochMillis(),
                importance,
                confidence,
                List.of(sourceEvent.id())
        ));
    }

    private static boolean sourceSupports(MemoryEvent.Provenance provenance, MemoryEvent sourceEvent) {
        return switch (provenance) {
            case PLAYER_TOLD, NPC_TOLD -> sourceEvent.type() == MemoryEvent.Type.DIALOGUE
                    && sourceEvent.provenance() == provenance;
            case INFERRED -> true;
            case SYSTEM_OBSERVED -> false;
        };
    }
}
