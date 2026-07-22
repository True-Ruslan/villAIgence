package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.knowledge.WorldEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Converts authoritative server-observed world events into actor-owned Memory 2.0 events. */
public final class WorldEventMemoryAdapter {
    private static final int ACTION_IMPORTANCE = 60;

    private WorldEventMemoryAdapter() {
    }

    public static Optional<MemoryEvent> toMemoryEvent(WorldEvent event, long createdAtEpochMillis) {
        if (event == null
                || event.type() != WorldEvent.Type.NPC_ACTION
                || event.provenance() != WorldEvent.Provenance.SYSTEM_OBSERVED
                || event.actorId() == null) {
            return Optional.empty();
        }

        List<UUID> participants = new ArrayList<>(2);
        participants.add(event.actorId());
        if (event.subjectId() != null) participants.add(event.subjectId());

        return Optional.of(new MemoryEvent(
                event.id(),
                event.actorId(),
                MemoryEvent.Type.ACTION,
                event.description(),
                participants,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                event.gameTime(),
                createdAtEpochMillis,
                ACTION_IMPORTANCE,
                0,
                100,
                List.of()
        ));
    }
}
