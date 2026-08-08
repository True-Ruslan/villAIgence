package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/** Admission boundary for relationship causes backed by exact persisted Memory 2.0 source events. */
public final class RelationshipCauseLifecycle {
    private RelationshipCauseLifecycle() {
    }

    public static Optional<MemoryEvent> recordDialogueTurn(
            boolean enabled,
            Path worldRoot,
            MemoryEvent relationshipChange,
            MemoryEvent dialogue,
            UUID playerId,
            int maxEventsPerNpc
    ) {
        if (!enabled || worldRoot == null || maxEventsPerNpc <= 0) return Optional.empty();

        Optional<MemoryEvent> candidate = RelationshipCauseMemoryAdapter.toDialogueTurnCause(
                relationshipChange,
                dialogue,
                playerId
        );
        if (candidate.isEmpty()) return Optional.empty();

        MemoryEventStore store = MemoryEventStore.forWorld(worldRoot);
        if (!containsExact(store, relationshipChange) || !containsExact(store, dialogue)) return Optional.empty();

        MemoryEvent cause = candidate.get();
        store.append(cause, maxEventsPerNpc);
        return Optional.of(cause);
    }

    private static boolean containsExact(MemoryEventStore store, MemoryEvent source) {
        if (source == null) return false;
        return store.getRecentMatching(
                        source.ownerNpcId(),
                        Integer.MAX_VALUE,
                        event -> source.id().equals(event.id())
                ).stream()
                .anyMatch(source::equals);
    }
}
