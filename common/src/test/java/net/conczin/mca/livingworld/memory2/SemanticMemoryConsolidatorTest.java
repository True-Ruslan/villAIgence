package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticMemoryConsolidatorTest {
    @Test
    void mergesIndependentEvidenceDeterministically() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();
        UUID sourceB = UUID.randomUUID();
        SemanticMemoryEntry first = fact(
                UUID.randomUUID(), npc, "  Village\tGate   OPEN  ",
                List.of(npc, player), 100L, 1000L, 60, 90, List.of(sourceA)
        );
        SemanticMemoryEntry second = fact(
                UUID.randomUUID(), npc, "village gate open",
                List.of(player, npc), 200L, 2000L, 80, 95, List.of(sourceB)
        );

        SemanticMemoryEntry forward = SemanticMemoryConsolidator.merge(first, second).orElseThrow();
        SemanticMemoryEntry reverse = SemanticMemoryConsolidator.merge(second, first).orElseThrow();

        assertEquals(forward, reverse);
        assertEquals(SemanticMemoryEntry.Kind.FACT, forward.kind());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, forward.provenance());
        assertEquals(200L, forward.gameTime());
        assertEquals(2000L, forward.createdAtEpochMillis());
        assertEquals(80, forward.importance());
        assertEquals(95, forward.confidence());
        assertEquals(sortedIds(npc, player), forward.relatedEntities());
        assertEquals(sortedIds(sourceA, sourceB), forward.sourceEventIds());
    }

    @Test
    void keepsSingleSourcedEntryIdentityUntilCorroborated() {
        UUID npc = UUID.randomUUID();
        SemanticMemoryEntry entry = fact(
                UUID.randomUUID(), npc, "Single observation", List.of(npc),
                10L, 20L, 50, 100, List.of(UUID.randomUUID())
        );

        assertEquals(List.of(entry), SemanticMemoryConsolidator.consolidateAll(List.of(entry)));
    }

    @Test
    void keepsTruthProvenanceEntityAndSourceBoundariesSeparate() {
        UUID npc = UUID.randomUUID();
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();
        UUID sourceB = UUID.randomUUID();
        SemanticMemoryEntry fact = fact(
                UUID.randomUUID(), npc, "The bridge is unsafe", List.of(npc, playerA),
                10L, 20L, 50, 100, List.of(sourceA)
        );
        SemanticMemoryEntry playerBelief = belief(
                UUID.randomUUID(), npc, "The bridge is unsafe", List.of(npc, playerA),
                MemoryEvent.Provenance.PLAYER_TOLD, List.of(sourceB)
        );
        SemanticMemoryEntry npcBelief = belief(
                UUID.randomUUID(), npc, "The bridge is unsafe", List.of(npc, playerA),
                MemoryEvent.Provenance.NPC_TOLD, List.of(sourceB)
        );
        SemanticMemoryEntry differentEntity = fact(
                UUID.randomUUID(), npc, "The bridge is unsafe", List.of(npc, playerB),
                10L, 20L, 50, 100, List.of(sourceB)
        );
        SemanticMemoryEntry unsourced = fact(
                UUID.randomUUID(), npc, "The bridge is unsafe", List.of(npc, playerA),
                10L, 20L, 50, 100, List.of()
        );

        assertTrue(SemanticMemoryConsolidator.merge(fact, playerBelief).isEmpty());
        assertTrue(SemanticMemoryConsolidator.merge(playerBelief, npcBelief).isEmpty());
        assertTrue(SemanticMemoryConsolidator.merge(fact, differentEntity).isEmpty());
        assertTrue(SemanticMemoryConsolidator.merge(fact, unsourced).isEmpty());
    }

    @Test
    void consolidatesReplayAndCorroborationIntoOneStableEntry() {
        UUID npc = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();
        UUID sourceB = UUID.randomUUID();
        SemanticMemoryEntry first = fact(
                UUID.randomUUID(), npc, "Observed a repaired door", List.of(npc),
                10L, 100L, 60, 100, List.of(sourceA)
        );
        SemanticMemoryEntry replay = new SemanticMemoryEntry(
                first.id(), first.ownerNpcId(), first.kind(), first.statement(), first.relatedEntities(),
                first.provenance(), first.gameTime(), first.createdAtEpochMillis(), first.importance(),
                first.confidence(), first.sourceEventIds()
        );
        SemanticMemoryEntry corroboration = fact(
                UUID.randomUUID(), npc, "observed a repaired door", List.of(npc),
                20L, 200L, 70, 100, List.of(sourceB)
        );

        List<SemanticMemoryEntry> consolidated = SemanticMemoryConsolidator.consolidateAll(
                List.of(corroboration, replay, first)
        );

        assertEquals(1, consolidated.size());
        assertEquals(sortedIds(sourceA, sourceB), consolidated.getFirst().sourceEventIds());
    }

    private static SemanticMemoryEntry fact(
            UUID id,
            UUID owner,
            String statement,
            List<UUID> relatedEntities,
            long gameTime,
            long createdAt,
            int importance,
            int confidence,
            List<UUID> sourceIds
    ) {
        return new SemanticMemoryEntry(
                id,
                owner,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                relatedEntities,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                createdAt,
                importance,
                confidence,
                sourceIds
        );
    }

    private static SemanticMemoryEntry belief(
            UUID id,
            UUID owner,
            String statement,
            List<UUID> relatedEntities,
            MemoryEvent.Provenance provenance,
            List<UUID> sourceIds
    ) {
        return new SemanticMemoryEntry(
                id,
                owner,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                relatedEntities,
                provenance,
                10L,
                20L,
                50,
                60,
                sourceIds
        );
    }

    private static List<UUID> sortedIds(UUID... ids) {
        List<UUID> values = new ArrayList<>(List.of(ids));
        values.sort(Comparator.comparing(UUID::toString));
        return List.copyOf(values);
    }
}
