package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlledSemanticMemoryIngestorContradictionTest {
    @TempDir
    Path tempDir;

    @Test
    void controlledFactAndBeliefAdmissionAutomaticallyRecordsOneTruthNeutralDisagreement() {
        Path world = tempDir.resolve("automatic");
        UUID npc = id(1);
        UUID player = id(90);
        UUID factSourceId = id(100);
        UUID beliefSourceId = id(101);

        ControlledSemanticMemoryIngestor.recordFact(
                world,
                observed(factSourceId, npc, player, "The gate is open", 100L),
                64
        );
        ControlledSemanticMemoryIngestor.recordBelief(
                world,
                new SemanticBeliefSource(
                        npc,
                        "The gate is not open",
                        List.of(npc, player),
                        MemoryEvent.Provenance.PLAYER_TOLD,
                        200L,
                        0L,
                        55,
                        61,
                        List.of(beliefSourceId)
                ),
                64
        );

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> history =
                SemanticContradictionHistory.load(world, npc, player, 8);
        assertEquals(1, history.size());

        List<SemanticMemoryEntry> semantic = SemanticMemoryStore.forWorld(world).getRecent(npc, 8);
        SemanticMemoryEntry fact = semantic.stream()
                .filter(entry -> entry.kind() == SemanticMemoryEntry.Kind.FACT)
                .findFirst()
                .orElseThrow();
        SemanticMemoryEntry belief = semantic.stream()
                .filter(entry -> entry.kind() == SemanticMemoryEntry.Kind.BELIEF)
                .findFirst()
                .orElseThrow();

        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, fact.provenance());
        assertEquals(100, fact.confidence());
        assertEquals(List.of(factSourceId), fact.sourceEventIds());
        assertEquals(MemoryEvent.Provenance.PLAYER_TOLD, belief.provenance());
        assertEquals(61, belief.confidence());
        assertEquals(List.of(beliefSourceId), belief.sourceEventIds());
    }

    @Test
    void replayedControlledAdmissionDoesNotCreateAnotherLiveRelation() {
        Path world = tempDir.resolve("replay");
        UUID npc = id(1);
        UUID player = id(90);
        MemoryEvent fact = observed(id(200), npc, player, "The gate is open", 100L);
        SemanticBeliefSource belief = new SemanticBeliefSource(
                npc,
                "The gate is not open",
                List.of(npc, player),
                MemoryEvent.Provenance.NPC_TOLD,
                200L,
                0L,
                50,
                50,
                List.of(id(201))
        );

        ControlledSemanticMemoryIngestor.recordFact(world, fact, 64);
        ControlledSemanticMemoryIngestor.recordBelief(world, belief, 64);
        ControlledSemanticMemoryIngestor.recordFact(world, fact, 64);
        ControlledSemanticMemoryIngestor.recordBelief(world, belief, 64);

        assertEquals(1, SemanticContradictionHistory.load(world, npc, player, 8).size());
        assertEquals(1, MemoryEventStore.forWorld(world).getRecentMatching(
                npc,
                64,
                event -> event.type() == MemoryEvent.Type.SEMANTIC_CONTRADICTION
                        && SemanticContradictionPolicy.valid(event)
        ).size());
    }

    @Test
    void trailingSentenceOmissionAloneDoesNotBecomeContradiction() {
        Path world = tempDir.resolve("transformation-boundary");
        UUID npc = id(1);
        UUID player = id(90);

        ControlledSemanticMemoryIngestor.recordFact(
                world,
                observed(id(300), npc, player, "The gate is open. A guard is nearby.", 100L),
                64
        );
        ControlledSemanticMemoryIngestor.recordBelief(
                world,
                new SemanticBeliefSource(
                        npc,
                        "The gate is open.",
                        List.of(npc, player),
                        MemoryEvent.Provenance.NPC_TOLD,
                        200L,
                        0L,
                        50,
                        50,
                        List.of(id(301))
                ),
                64
        );

        assertTrue(SemanticContradictionHistory.load(world, npc, player, 8).isEmpty());
    }

    private static MemoryEvent observed(
            UUID eventId,
            UUID npc,
            UUID player,
            String summary,
            long gameTime
    ) {
        return new MemoryEvent(
                eventId,
                npc,
                MemoryEvent.Type.OBSERVATION,
                summary,
                List.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                90,
                0,
                100,
                List.of()
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
