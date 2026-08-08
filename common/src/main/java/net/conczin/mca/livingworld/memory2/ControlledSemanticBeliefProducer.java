package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Persists only BELIEF inputs that pass the explicit admission policy. */
public final class ControlledSemanticBeliefProducer {
    private ControlledSemanticBeliefProducer() {
    }

    public static void recordIfEnabled(
            boolean enabled,
            Path worldRoot,
            MemoryEvent sourceEvent,
            String statement,
            List<UUID> relatedEntities,
            MemoryEvent.Provenance provenance,
            int importance,
            int confidence,
            int maxEntriesPerNpc
    ) {
        if (!enabled || worldRoot == null) return;

        SemanticBeliefAdmissionPolicy.admit(
                sourceEvent,
                statement,
                relatedEntities,
                provenance,
                importance,
                confidence
        ).ifPresent(source -> ControlledSemanticMemoryIngestor.recordBelief(
                worldRoot,
                source,
                maxEntriesPerNpc
        ));
    }
}
