package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.UUID;

/** Server-owned orchestration boundary for one NPC-to-NPC knowledge-transfer attempt. */
public final class NpcKnowledgeTransferLifecycle {
    private NpcKnowledgeTransferLifecycle() {
    }

    public static NpcKnowledgeTransferResult transfer(
            Path worldRoot,
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            long authoritativeGameTime,
            int memory2CapacityPerNpc,
            int semanticCapacityPerNpc
    ) {
        return new NpcKnowledgeTransferResult(
                NpcKnowledgeTransferResult.Status.REJECTED,
                null,
                null
        );
    }
}
