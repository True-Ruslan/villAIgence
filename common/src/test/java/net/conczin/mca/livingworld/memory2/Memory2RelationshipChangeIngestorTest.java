package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipChange;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Memory2RelationshipChangeIngestorTest {
    @TempDir Path tempDir;

    @Test
    void duplicateReplayIsIdempotent() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        LivingWorldRelationshipChange change = LivingWorldRelationshipChange.between(
                new LivingWorldRelationshipState(1, 2, 3, 4),
                new LivingWorldRelationshipState(3, 1, 3, 5));

        Memory2RelationshipChangeIngestor.record(tempDir, npc, player, 100L, change, 16, 1000L);
        Memory2RelationshipChangeIngestor.record(tempDir, npc, player, 100L, change, 16, 9999L);

        assertEquals(1, MemoryEventStore.forWorld(tempDir).getRecent(npc, 16).size());
    }

    @Test
    void retentionIsBoundedForDistinctTransitions() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        LivingWorldRelationshipState zero = LivingWorldRelationshipState.NEUTRAL;
        LivingWorldRelationshipState one = new LivingWorldRelationshipState(1, 0, 0, 0);
        LivingWorldRelationshipState two = new LivingWorldRelationshipState(2, 1, 0, 0);
        LivingWorldRelationshipState three = new LivingWorldRelationshipState(2, 1, 1, -1);

        Memory2RelationshipChangeIngestor.record(tempDir, npc, player, 100L, LivingWorldRelationshipChange.between(zero, one), 2, 1000L);
        Memory2RelationshipChangeIngestor.record(tempDir, npc, player, 101L, LivingWorldRelationshipChange.between(one, two), 2, 2000L);
        Memory2RelationshipChangeIngestor.record(tempDir, npc, player, 102L, LivingWorldRelationshipChange.between(two, three), 2, 3000L);

        assertEquals(2, MemoryEventStore.forWorld(tempDir).getRecent(npc, 16).size());
    }

    @Test
    void disabledOrUnchangedTransitionCreatesNoMemory() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        LivingWorldRelationshipState state = new LivingWorldRelationshipState(1, 2, 3, 4);
        LivingWorldRelationshipChange changed = LivingWorldRelationshipChange.between(state, new LivingWorldRelationshipState(2, 2, 3, 4));
        LivingWorldRelationshipChange unchanged = LivingWorldRelationshipChange.between(state, state);

        Memory2RelationshipChangeIngestor.recordIfEnabled(false, tempDir, npc, player, 100L, changed, 16, 1000L);
        Memory2RelationshipChangeIngestor.recordIfEnabled(true, tempDir, npc, player, 101L, unchanged, 16, 2000L);

        assertEquals(List.of(), MemoryEventStore.forWorld(tempDir).getRecent(npc, 16));
    }
}
