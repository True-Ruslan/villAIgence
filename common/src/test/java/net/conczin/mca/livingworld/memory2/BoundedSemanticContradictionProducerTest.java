package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedSemanticContradictionProducerTest {
    @TempDir
    Path tempDir;

    @Test
    void hardComparisonBudgetStopsAtEightAfterSixteenCandidateSelection() {
        Path world = tempDir.resolve("budget");
        UUID npc = id(1);
        UUID player = id(90);
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);

        for (int i = 0; i < 20; i++) {
            store.append(belief(id(100 + i), npc, "Distinct candidate " + i, player, 100L + i), 64);
        }
        SemanticMemoryEntry subject = fact(id(200), npc, "The gate is open", player, 1_000L);
        store.append(subject, 64);

        BoundedSemanticContradictionProducer.ProductionResult result =
                BoundedSemanticContradictionProducer.produce(world, subject, 64);

        assertEquals(SemanticContradictionCandidateSelector.MAX_CANDIDATES_PER_ADMISSION,
                result.eligibleCandidates());
        assertEquals(SemanticContradictionCandidateSelector.MAX_COMPARISONS_PER_ADMISSION,
                result.comparisons());
        assertEquals(0, result.oppositions());
        assertTrue(result.recordedEventIds().isEmpty());
        assertTrue(MemoryEventStore.forWorld(world).getRecent(npc, 64).isEmpty());
    }

    @Test
    void recordsExplicitOppositionOnceAndSuppressesExistingRelationBeforeComparison() {
        Path world = tempDir.resolve("replay");
        UUID npc = id(1);
        UUID player = id(90);
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        SemanticMemoryEntry candidate = belief(id(300), npc, "The gate is not open", player, 100L);
        SemanticMemoryEntry subject = fact(id(301), npc, "The gate is open", player, 200L);
        store.append(candidate, 64);
        store.append(subject, 64);

        BoundedSemanticContradictionProducer.ProductionResult first =
                BoundedSemanticContradictionProducer.produce(world, subject, 64);

        assertEquals(1, first.eligibleCandidates());
        assertEquals(1, first.comparisons());
        assertEquals(1, first.oppositions());
        assertEquals(1, first.recordedEventIds().size());
        assertEquals(1, SemanticContradictionHistory.load(world, npc, player, 8).size());

        BoundedSemanticContradictionProducer.ProductionResult replay =
                BoundedSemanticContradictionProducer.produce(world, subject, 64);

        assertEquals(1, replay.eligibleCandidates());
        assertEquals(0, replay.comparisons());
        assertEquals(0, replay.oppositions());
        assertTrue(replay.recordedEventIds().isEmpty());
        assertEquals(1, SemanticContradictionHistory.load(world, npc, player, 8).size());
    }

    @Test
    void existingNonSubjectRelationDoesNotConsumeOrSuppressSubjectComparison() {
        Path world = tempDir.resolve("pair-identity");
        UUID npc = id(1);
        UUID player = id(90);
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);

        SemanticMemoryEntry otherPositive = fact(id(400), npc, "The mill is running", player, 10L);
        SemanticMemoryEntry otherNegative = belief(id(401), npc, "The mill is not running", player, 20L);
        SemanticMemoryEntry subject = fact(id(402), npc, "The gate is open", player, 30L);
        SemanticMemoryEntry subjectNegative = belief(id(403), npc, "The gate is not open", player, 40L);
        store.append(otherPositive, 64);
        store.append(otherNegative, 64);
        store.append(subject, 64);
        store.append(subjectNegative, 64);

        assertEquals(
                SemanticContradictionResult.Status.RECORDED,
                SemanticContradictionLifecycle.record(
                        world,
                        npc,
                        otherPositive.id(),
                        otherNegative.id(),
                        50L,
                        64
                ).status()
        );

        BoundedSemanticContradictionProducer.ProductionResult result =
                BoundedSemanticContradictionProducer.produce(world, subject, 64);

        assertTrue(result.comparisons() > 0);
        assertEquals(1, result.oppositions());
        assertEquals(1, result.recordedEventIds().size());
        assertEquals(2, SemanticContradictionHistory.load(world, npc, player, 8).size());
    }

    private static SemanticMemoryEntry fact(UUID entryId, UUID npc, String statement, UUID player, long gameTime) {
        return new SemanticMemoryEntry(
                entryId,
                npc,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                90,
                100,
                List.of(id(7_000 + (int) gameTime))
        );
    }

    private static SemanticMemoryEntry belief(UUID entryId, UUID npc, String statement, UUID player, long gameTime) {
        return new SemanticMemoryEntry(
                entryId,
                npc,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                List.of(player),
                MemoryEvent.Provenance.NPC_TOLD,
                gameTime,
                0L,
                50,
                50,
                List.of(id(8_000 + (int) gameTime))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
