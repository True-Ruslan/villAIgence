package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipChange;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationshipChangeMemoryAdapterTest {
    @Test
    void mapsPersistedRelationshipTransitionToServerObservedMemory() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        LivingWorldRelationshipChange change = LivingWorldRelationshipChange.between(
                new LivingWorldRelationshipState(10, 4, 1, 7),
                new LivingWorldRelationshipState(12, 3, 0, 8)
        );

        MemoryEvent event = RelationshipChangeMemoryAdapter.toMemoryEvent(
                npc,
                player,
                1234L,
                change,
                1_700_000_000_123L
        ).orElseThrow();

        assertEquals(npc, event.ownerNpcId());
        assertEquals(MemoryEvent.Type.RELATIONSHIP_CHANGE, event.type());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, event.provenance());
        assertEquals(List.of(npc, player), event.participants());
        assertEquals(1234L, event.gameTime());
        assertEquals(1_700_000_000_123L, event.createdAtEpochMillis());
        assertEquals(55, event.importance());
        assertEquals(0, event.emotionalWeight());
        assertEquals(100, event.confidence());
        assertEquals(List.of(), event.relationshipReasons());
        assertEquals(
                "Relationship with player changed: trust +2, respect -1, fear -1, affinity +1; now trust=12, respect=3, fear=0, affinity=8.",
                event.summary()
        );
    }

    @Test
    void deterministicIdUsesTransitionAndGameTimeButNotWallClock() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        LivingWorldRelationshipChange change = LivingWorldRelationshipChange.between(
                new LivingWorldRelationshipState(1, 2, 3, 4),
                new LivingWorldRelationshipState(2, 2, 1, 5)
        );
        LivingWorldRelationshipChange differentTransition = LivingWorldRelationshipChange.between(
                new LivingWorldRelationshipState(1, 2, 3, 4),
                new LivingWorldRelationshipState(3, 2, 1, 5)
        );

        MemoryEvent first = RelationshipChangeMemoryAdapter.toMemoryEvent(npc, player, 40L, change, 1_000L).orElseThrow();
        MemoryEvent replay = RelationshipChangeMemoryAdapter.toMemoryEvent(npc, player, 40L, change, 9_999L).orElseThrow();
        MemoryEvent later = RelationshipChangeMemoryAdapter.toMemoryEvent(npc, player, 41L, change, 1_000L).orElseThrow();
        MemoryEvent other = RelationshipChangeMemoryAdapter.toMemoryEvent(npc, player, 40L, differentTransition, 1_000L).orElseThrow();

        assertEquals(first.id(), replay.id());
        assertNotEquals(first.id(), later.id());
        assertNotEquals(first.id(), other.id());
    }

    @Test
    void rejectsMissingIdentifiersOrUnchangedTransitions() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        LivingWorldRelationshipState state = new LivingWorldRelationshipState(1, 2, 3, 4);
        LivingWorldRelationshipChange unchanged = LivingWorldRelationshipChange.between(state, state);

        assertTrue(RelationshipChangeMemoryAdapter.toMemoryEvent(null, player, 1L, unchanged, 1L).isEmpty());
        assertTrue(RelationshipChangeMemoryAdapter.toMemoryEvent(npc, null, 1L, unchanged, 1L).isEmpty());
        assertTrue(RelationshipChangeMemoryAdapter.toMemoryEvent(npc, player, 1L, null, 1L).isEmpty());
        assertTrue(RelationshipChangeMemoryAdapter.toMemoryEvent(npc, player, 1L, unchanged, 1L).isEmpty());
    }
}
