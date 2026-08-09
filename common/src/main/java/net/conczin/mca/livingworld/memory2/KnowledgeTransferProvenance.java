package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Immutable persisted ancestry snapshot for one server-owned NPC-to-NPC transfer event. */
public record KnowledgeTransferProvenance(
        Origin origin,
        List<Hop> hops
) {
    public KnowledgeTransferProvenance {
        hops = immutableCopyPreservingNulls(hops);
    }

    public record Origin(
            UUID originNpcId,
            UUID originSemanticEntryId,
            SemanticMemoryEntry.Kind originKind,
            MemoryEvent.Provenance originProvenance,
            String statement,
            List<UUID> relatedEntities
    ) {
        public Origin {
            relatedEntities = immutableCopyPreservingNulls(relatedEntities);
        }
    }

    public record Hop(
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            UUID evidenceEventId,
            long gameTime
    ) {
    }

    private static <T> List<T> immutableCopyPreservingNulls(List<T> values) {
        return values == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
