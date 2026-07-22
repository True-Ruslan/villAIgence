package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.UUID;

/** Persists eligible successful dialogue turns into the NPC's Memory 2.0 store. */
public final class Memory2DialogueIngestor {
    private Memory2DialogueIngestor() {
    }

    public static void record(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            String playerMessage,
            String npcReply,
            int maxEventsPerNpc,
            long createdAtEpochMillis
    ) {
        if (worldRoot == null) return;
        DialogueMemoryAdapter.toMemoryEvent(
                npcId,
                playerId,
                gameTime,
                playerMessage,
                npcReply,
                createdAtEpochMillis
        ).ifPresent(memory -> MemoryEventStore.forWorld(worldRoot).append(memory, maxEventsPerNpc));
    }
}
