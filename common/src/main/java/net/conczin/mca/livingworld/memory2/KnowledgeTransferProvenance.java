package net.conczin.mca.livingworld.memory2;

import java.util.List;
import java.util.UUID;

/** Immutable persisted ancestry snapshot for one server-owned NPC-to-NPC transfer event. */
public record KnowledgeTransferProvenance(
        Origin origin,
        List<Hop> hops
) {
    public record Origin(
            UUID originNpcId,
            UUID originSemanticEntryId,
            SemanticMemoryEntry.Kind originKind,
            MemoryEvent.Provenance originProvenance,
            String statement,
            List<UUID> relatedEntities
    ) {
    }

    public record Hop(
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            UUID evidenceEventId,
            long gameTime
    ) {
    }
}
