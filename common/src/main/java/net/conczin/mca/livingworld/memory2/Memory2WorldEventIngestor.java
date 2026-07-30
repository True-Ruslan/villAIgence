package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.knowledge.WorldEvent;

import java.nio.file.Path;

/** Persists eligible authoritative WorldEvents into the actor NPC's Memory 2.0 stores. */
public final class Memory2WorldEventIngestor {
    private Memory2WorldEventIngestor() {
    }

    public static void record(
            Path worldRoot,
            WorldEvent event,
            int maxEventsPerNpc,
            long createdAtEpochMillis
    ) {
        record(worldRoot, event, maxEventsPerNpc, false, 1, createdAtEpochMillis);
    }

    public static void record(
            Path worldRoot,
            WorldEvent event,
            int maxEventsPerNpc,
            boolean semanticEnabled,
            int maxSemanticEntriesPerNpc,
            long createdAtEpochMillis
    ) {
        if (worldRoot == null || event == null) return;
        WorldEventMemoryAdapter.toMemoryEvent(event, createdAtEpochMillis).ifPresent(memory -> {
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
