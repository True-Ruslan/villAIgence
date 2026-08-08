package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipChange;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationshipCausalHistoryTest {
    @TempDir Path tempDir;

    @Test
    void filtersExactNpcAndPlayerBeforeLimitingAndReturnsNewestFirst() {
        UUID npc = UUID.randomUUID();
        UUID otherNpc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();

        MemoryEvent older = persistCause(tempDir, npc, player, 100L, 1_000L, 32);
        persistCause(tempDir, npc, otherPlayer, 300L, 3_000L, 32);
        MemoryEvent newer = persistCause(tempDir, npc, player, 200L, 2_000L, 32);
        persistCause(tempDir, otherNpc, player, 400L, 4_000L, 32);

        List<ResolvedRelationshipCause> history = RelationshipCausalHistory.getRecent(
                tempDir, npc, player, 2
        );

        assertEquals(2, history.size());
        assertEquals(newer.id(), history.get(0).causeEvent().id());
        assertEquals(older.id(), history.get(1).causeEvent().id());
        assertTrue(history.stream().allMatch(item -> item.causeEvent().ownerNpcId().equals(npc)));
        assertTrue(history.stream().allMatch(item -> item.causeEvent().participants().contains(player)));
    }

    @Test
    void resolvesExactSourcesWhenTheyAreStillPresent() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent cause = persistCause(tempDir, npc, player, 100L, 1_000L, 16);

        ResolvedRelationshipCause resolved = RelationshipCausalHistory.getRecent(
                tempDir, npc, player, 1
        ).getFirst();

        assertEquals(cause.id(), resolved.causeEvent().id());
        assertEquals(cause.relationshipCause().transitionSnapshot(), resolved.transition());
        assertEquals(cause.relationshipCause().relationshipChangeEventId(), resolved.relationshipChangeEventId());
        assertEquals(cause.relationshipCause().evidenceEventId(), resolved.evidenceEventId());
        assertTrue(resolved.relationshipChangeEvent().isPresent());
        assertTrue(resolved.evidenceEvent().isPresent());
        assertEquals(MemoryEvent.Type.RELATIONSHIP_CHANGE, resolved.relationshipChangeEvent().orElseThrow().type());
        assertEquals(MemoryEvent.Type.DIALOGUE, resolved.evidenceEvent().orElseThrow().type());
    }

    @Test
    void sourceEvictionKeepsIdsAndTransitionWithoutFabricatingResolvedEvidence() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent cause = persistCause(tempDir, npc, player, 100L, 1_000L, 1);

        List<MemoryEvent> remaining = MemoryEventStore.forWorld(tempDir).getRecent(npc, 8);
        assertEquals(List.of(cause), remaining);

        ResolvedRelationshipCause resolved = RelationshipCausalHistory.getRecent(
                tempDir, npc, player, 1
        ).getFirst();

        assertEquals(cause.relationshipCause().relationshipChangeEventId(), resolved.relationshipChangeEventId());
        assertEquals(cause.relationshipCause().evidenceEventId(), resolved.evidenceEventId());
        assertEquals(cause.relationshipCause().transitionSnapshot(), resolved.transition());
        assertTrue(resolved.relationshipChangeEvent().isEmpty());
        assertTrue(resolved.evidenceEvent().isEmpty());
        assertFalse(resolved.causeEvent().summary().contains("because"));
    }

    @Test
    void restartRoundTripRetainsStructuredCausePayloadAndSourceIds() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent cause = persistCause(tempDir, npc, player, 100L, 1_000L, 16);
        Path file = tempDir.resolve("livingworld").resolve("memory2.json");

        MemoryEventStore restarted = new MemoryEventStore(file);
        MemoryEvent restored = restarted.getRecentMatching(
                npc,
                16,
                event -> event.type() == MemoryEvent.Type.RELATIONSHIP_CAUSE
        ).getFirst();

        assertEquals(cause.id(), restored.id());
        assertEquals(cause.relationshipCause(), restored.relationshipCause());
        assertEquals(cause.relationshipCause().relationshipChangeEventId(), restored.relationshipCause().relationshipChangeEventId());
        assertEquals(cause.relationshipCause().evidenceEventId(), restored.relationshipCause().evidenceEventId());
        assertEquals(cause.relationshipCause().transitionSnapshot(), restored.relationshipCause().transitionSnapshot());
    }

    @Test
    void invalidQueryCoordinatesFailClosed() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        persistCause(tempDir, npc, player, 100L, 1_000L, 16);

        assertEquals(List.of(), RelationshipCausalHistory.getRecent(null, npc, player, 1));
        assertEquals(List.of(), RelationshipCausalHistory.getRecent(tempDir, null, player, 1));
        assertEquals(List.of(), RelationshipCausalHistory.getRecent(tempDir, npc, null, 1));
        assertEquals(List.of(), RelationshipCausalHistory.getRecent(tempDir, npc, player, 0));
    }

    private static MemoryEvent persistCause(
            Path worldRoot,
            UUID npc,
            UUID player,
            long gameTime,
            long createdAtEpochMillis,
            int maxEventsPerNpc
    ) {
        LivingWorldRelationshipChange change = LivingWorldRelationshipChange.between(
                new LivingWorldRelationshipState((int) (gameTime % 5), 0, 0, 0),
                new LivingWorldRelationshipState((int) (gameTime % 5) + 1, 1, 0, -1)
        );
        MemoryEvent relationship = RelationshipChangeMemoryAdapter.toMemoryEvent(
                npc, player, gameTime, change, createdAtEpochMillis
        ).orElseThrow();
        MemoryEvent dialogue = new MemoryEvent(
                UUID.nameUUIDFromBytes((npc + ":" + player + ":dialogue:" + gameTime).getBytes()),
                npc,
                MemoryEvent.Type.DIALOGUE,
                "Dialogue with player.",
                List.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                createdAtEpochMillis + 1,
                40,
                0,
                100,
                List.of(),
                new MemoryEvent.DialogueExchange("hello " + gameTime, "reply " + gameTime)
        );

        MemoryEventStore store = MemoryEventStore.forWorld(worldRoot);
        store.append(relationship, Math.max(3, maxEventsPerNpc));
        store.append(dialogue, Math.max(3, maxEventsPerNpc));
        return RelationshipCauseLifecycle.recordDialogueTurn(
                true,
                worldRoot,
                relationship,
                dialogue,
                player,
                maxEventsPerNpc
        ).orElseThrow();
    }
}
