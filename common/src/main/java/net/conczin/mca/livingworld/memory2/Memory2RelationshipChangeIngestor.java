package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipChange;

import java.nio.file.Path;
import java.util.Optional;
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
        recordAndReturnIfEnabled(
                enabled,
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
        recordAndReturnIfEnabled(
                enabled,
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

    public static Optional<MemoryEvent> recordAndReturnIfEnabled(
            boolean enabled,
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            LivingWorldRelationshipChange change,
            int maxEventsPerNpc,
            long createdAtEpochMillis
    ) {
        return recordAndReturnIfEnabled(
                enabled,
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

    public static Optional<MemoryEvent> recordAndReturnIfEnabled(
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
        if (!enabled) return Optional.empty();
        return recordAndReturn(
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
        recordAndReturn(
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
        recordAndReturn(
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

    public static Optional<MemoryEvent> recordAndReturn(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            LivingWorldRelationshipChange change,
            int maxEventsPerNpc,
            long createdAtEpochMillis
    ) {
        return recordAndReturn(
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

    public static Optional<MemoryEvent> recordAndReturn(
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
        if (worldRoot == null) return Optional.empty();
        Optional<MemoryEvent> event = RelationshipChangeMemoryAdapter.toMemoryEvent(
                npcId,
                playerId,
                gameTime,
                change,
                createdAtEpochMillis
        );
        if (event.isEmpty()) return Optional.empty();

        MemoryEvent proposed = event.get();
        MemoryEventStore store = MemoryEventStore.forWorld(worldRoot);
        store.append(proposed, maxEventsPerNpc);
        Optional<MemoryEvent> persisted = store.getRecentMatching(
                        proposed.ownerNpcId(),
                        Integer.MAX_VALUE,
                        candidate -> proposed.id().equals(candidate.id())
                ).stream()
                .findFirst();
        if (persisted.isEmpty()) return Optional.empty();

        MemoryEvent memory = persisted.get();
        ControlledSemanticMemoryIngestor.recordFactIfEnabled(
                semanticEnabled,
                worldRoot,
                memory,
                maxSemanticEntriesPerNpc
        );
        return Optional.of(memory);
    }
}
