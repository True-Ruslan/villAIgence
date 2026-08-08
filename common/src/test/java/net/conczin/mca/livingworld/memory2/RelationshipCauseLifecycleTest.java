package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipChange;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationshipCauseLifecycleTest {
    @TempDir Path tempDir;

    @Test
    void persistedMatchingSourcesCreateOneIdempotentCause() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent relationship = relationshipChange(npc, player, 100L);
        MemoryEvent dialogue = dialogue(npc, player, 100L);
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);
        store.append(relationship, 16);
        store.append(dialogue, 16);

        MemoryEvent first = RelationshipCauseLifecycle.recordDialogueTurn(
                true, tempDir, relationship, dialogue, player, 16
        ).orElseThrow();
        MemoryEvent replay = RelationshipCauseLifecycle.recordDialogueTurn(
                true, tempDir, relationship, dialogue, player, 16
        ).orElseThrow();

        assertEquals(first.id(), replay.id());
        List<MemoryEvent> causes = store.getRecent(npc, 16).stream()
                .filter(event -> event.type() == MemoryEvent.Type.RELATIONSHIP_CAUSE)
                .toList();
        assertEquals(1, causes.size());
        assertEquals(first.id(), causes.getFirst().id());
    }

    @Test
    void rejectsDisabledOrUnpersistedSources() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        MemoryEvent relationship = relationshipChange(npc, player, 100L);
        MemoryEvent dialogue = dialogue(npc, player, 100L);
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);
        store.append(relationship, 16);

        assertTrue(RelationshipCauseLifecycle.recordDialogueTurn(
                false, tempDir, relationship, dialogue, player, 16
        ).isEmpty());
        assertTrue(RelationshipCauseLifecycle.recordDialogueTurn(
                true, tempDir, relationship, dialogue, player, 16
        ).isEmpty());
        assertEquals(0, store.getRecent(npc, 16).stream()
                .filter(event -> event.type() == MemoryEvent.Type.RELATIONSHIP_CAUSE)
                .count());
    }

    private static MemoryEvent relationshipChange(UUID npc, UUID player, long gameTime) {
        return RelationshipChangeMemoryAdapter.toMemoryEvent(
                npc,
                player,
                gameTime,
                LivingWorldRelationshipChange.between(
                        LivingWorldRelationshipState.NEUTRAL,
                        new LivingWorldRelationshipState(2, 1, 0, -1)
                ),
                1_000L
        ).orElseThrow();
    }

    private static MemoryEvent dialogue(UUID npc, UUID player, long gameTime) {
        return new MemoryEvent(
                UUID.randomUUID(),
                npc,
                MemoryEvent.Type.DIALOGUE,
                "Dialogue with player.",
                List.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                2_000L,
                40,
                0,
                100,
                List.of(),
                new MemoryEvent.DialogueExchange("hello", "hi")
        );
    }
}
