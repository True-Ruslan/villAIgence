package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.knowledge.WorldEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldEventMemoryAdapterTest {
    @Test
    void mapsAuthoritativeNpcActionToActorOwnedMemory() {
        UUID eventId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        WorldEvent source = new WorldEvent(
                eventId,
                WorldEvent.Type.NPC_ACTION,
                "Ruslan asked Alex to follow him",
                WorldEvent.Provenance.SYSTEM_OBSERVED,
                "minecraft:overworld",
                10, 64, 20,
                1234L,
                actor,
                subject
        );

        MemoryEvent memory = WorldEventMemoryAdapter.toMemoryEvent(source, 1_700_000_000_123L).orElseThrow();

        assertEquals(eventId, memory.id());
        assertEquals(actor, memory.ownerNpcId());
        assertEquals(MemoryEvent.Type.ACTION, memory.type());
        assertEquals(source.description(), memory.summary());
        assertEquals(List.of(actor, subject), memory.participants());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, memory.provenance());
        assertEquals(1234L, memory.gameTime());
        assertEquals(1_700_000_000_123L, memory.createdAtEpochMillis());
        assertEquals(60, memory.importance());
        assertEquals(0, memory.emotionalWeight());
        assertEquals(100, memory.confidence());
        assertEquals(List.of(), memory.relationshipReasons());
    }

    @Test
    void rejectsMissingActorAndNormalizesDuplicateParticipants() {
        UUID same = UUID.randomUUID();
        WorldEvent noActor = new WorldEvent(
                UUID.randomUUID(), WorldEvent.Type.NPC_ACTION, "event",
                WorldEvent.Provenance.SYSTEM_OBSERVED, "minecraft:overworld",
                0, 64, 0, 10L, null, same
        );
        assertTrue(WorldEventMemoryAdapter.toMemoryEvent(noActor, 1L).isEmpty());
        assertTrue(WorldEventMemoryAdapter.toMemoryEvent(null, 1L).isEmpty());

        WorldEvent sameActorAndSubject = new WorldEvent(
                UUID.randomUUID(), WorldEvent.Type.NPC_ACTION, "event",
                WorldEvent.Provenance.SYSTEM_OBSERVED, "minecraft:overworld",
                0, 64, 0, 10L, same, same
        );
        MemoryEvent memory = WorldEventMemoryAdapter.toMemoryEvent(sameActorAndSubject, 1L).orElseThrow();
        assertEquals(List.of(same), memory.participants());
    }
}
