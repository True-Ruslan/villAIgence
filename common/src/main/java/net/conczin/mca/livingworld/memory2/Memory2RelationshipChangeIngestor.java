package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipChange;

import java.nio.file.Path;
import java.util.UUID;

/** Persists eligible server-observed relationship transitions into the NPC's Memory 2.0 stores. */
public final class Memory2RelationshipChangeIngestor {
    private Memory2RelationshipChangeIngestor() {
    }

    public static void recordIfEnabled(
            boolean enabled,
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            LivingWorldRelationshipChange change,
            int maxEventsPerNpc,
            long createdAtEpochMillis
    ) {
        if (!enabled) return;
        record(
                worldRoot,
                npcId,
                playerId,
                gameTime,
                change,
                maxEventsPerNpc,
                true,
                maxEventsPerNpc,
                createdAtEpochMillis
        );
    }

    public static void recordIfEnabled(
            boolean enabled,
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            LivingWorldRelationshipChange change,
            int maxEventsPerNpc,
            boolean semanticEnabled,
            int maxSemanticEntriesPerNpc,
            long createdAtEpochMillis
    ) {
        if (!enabled) return;
        record(
                worldRoot,
                npcId,
                playerId,
                gameTime,
                change,
                maxEventsPerNpc,
                semanticEnabled,
                maxSemanticEntriesPerNpc,
                createdAtEpochMillis
        );
    }

    public static void record(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            LivingWorldRelationshipChange change,
            int maxEventsPerNpc,
            long createdAtEpochMillis
    ) {
        record(
                worldRoot,
                npcId,
                playerId,
                gameTime,
                change,
                maxEventsPerNpc,
                true,
                maxEventsPerNpc,
                createdAtEpochMillis
        );
    }

    public static void record(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            LivingWorldRelationshipChange change,
            int maxEventsPerNpc,
            boolean semanticEnabled,
            int maxSemanticEntriesPerNpc,
            long createdAtEpochMillis
    ) {
        if (worldRoot == null) return;
        RelationshipChangeMemoryAdapter.toMemoryEvent(
                npcId,
                playerId,
                gameTime,
                change,
                createdAtEpochMillis
        ).ifPresent(memory -> {
            MemoryEventStore.forWorld(worldRoot).append(memory, maxEventsPerNpc);
            ControlledSemanticMemoryIngestor.recordFactIfEnabled(
                    semanticEnabled,
                    worldRoot,
                    memory,
                    maxSemanticEntriesPerNpc
            );
        });
    }
}
