package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/** Shared post-success admission boundary for persistent dialogue Memory 2.0 events. */
public final class Memory2DialogueLifecycle {
    private Memory2DialogueLifecycle() {
    }

    public static void recordSuccessful(
            boolean enabled,
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            String playerMessage,
            Optional<String> answer,
            int maxEventsPerNpc,
            long createdAtEpochMillis
    ) {
        if (!enabled || answer == null || answer.isEmpty()) return;
        String npcReply = answer.get();
        if (npcReply == null || npcReply.isBlank()) return;

        Memory2DialogueIngestor.recordIfEnabled(
                true,
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
}