package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/** Persists eligible successful dialogue turns into the NPC's Memory 2.0 store. */
public final class Memory2DialogueIngestor {
    private Memory2DialogueIngestor() {
    }

    public static Optional<MemoryEvent> recordIfEnabled(
            boolean enabled,
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            String playerMessage,
            String npcReply,
            int maxEventsPerNpc,
            long createdAtEpochMillis
    ) {
        if (!enabled) return Optional.empty();
        return record(
                worldRoot,
                npcId,
                playerId,
                gameTime,
                playerMessage,
                npcReply,
                maxEventsPerNpc,
                createdAtEpochMillis
        );
    }

    public static Optional<MemoryEvent> record(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            String playerMessage,
            String npcReply,
            int maxEventsPerNpc,
            long createdAtEpochMillis
    ) {
        if (worldRoot == null) return Optional.empty();
        Optional<MemoryEvent> source = DialogueMemoryAdapter.toMemoryEvent(
                npcId,
                playerId,
                gameTime,
                playerMessage,
                npcReply,
                createdAtEpochMillis
        );
        source.ifPresent(memory -> MemoryEventStore.forWorld(worldRoot).append(memory, maxEventsPerNpc));
        return source;
    }
}
