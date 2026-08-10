package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementKnowledgeFlowPreservationTest {
    @TempDir
    Path tempDir;

    @Test
    void playerPrivateScopeRemainsExactAndForeignPlayerEligibilityStaysClosed() {
        Path world = tempDir.resolve("privacy");
        UUID speaker = id(1);
        UUID listener = id(2);
        UUID ownerPlayer = id(90);
        UUID foreignPlayer = id(91);
        SemanticMemoryStore.forWorld(world).append(fact(
                id(100), speaker, "The east cache is hidden", List.of(ownerPlayer), 100L), 64);

        SettlementKnowledgeFlowLifecycle.CycleResult result = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 3, 2_400L, List.of(speaker, listener), 64, 64);

        assertEquals(1, result.successfulTransfers());
        SemanticMemoryEntry transferred = SemanticMemoryStore.forWorld(world).getRecent(listener, 64).getFirst();
        assertEquals(List.of(ownerPlayer), transferred.relatedEntities());
        assertTrue(PlayerScopedMemoryEligibility.semantic(transferred, listener, ownerPlayer));
        assertFalse(PlayerScopedMemoryEligibility.semantic(transferred, listener, foreignPlayer));
    }

    @Test
    void settlementPropagationAppendsExistingV2LineageAndCarriesTransformationUnchanged() {
        Path world = tempDir.resolve("provenance-transform");
        UUID npcA = id(10);
        UUID npcB = id(11);
        UUID npcC = id(12);
        UUID player = id(92);
        UUID sourceId = id(110);
        SemanticMemoryStore.forWorld(world).append(fact(
                sourceId,
                npcA,
                "The western road is flooded. The old bridge is closed.",
                List.of(player),
                100L
        ), 64);

        NpcKnowledgeTransferResult ab = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                world, npcA, npcB, sourceId, 1_200L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, ab.status());
        MemoryEvent abEvidence = MemoryEventStore.forWorld(world)
                .findById(npcB, ab.evidenceEventId()).orElseThrow();
        assertNotNull(abEvidence.knowledgeTransferTransformation());
        assertEquals(1, abEvidence.knowledgeTransferTransformation().steps().size());

        SettlementKnowledgeFlowLifecycle.CycleResult bc = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 4, 3_600L, List.of(npcB, npcC), 64, 64);
        assertEquals(1, bc.successfulTransfers());

        MemoryEvent bcEvidence = MemoryEventStore.forWorld(world).getRecent(npcC, 64).stream()
                .filter(event -> event.type() == MemoryEvent.Type.DIALOGUE
                        && event.provenance() == MemoryEvent.Provenance.NPC_TOLD)
                .findFirst().orElseThrow();
        KnowledgeTransferProvenance lineage = bcEvidence.knowledgeTransferProvenance();
        assertNotNull(lineage);
        assertEquals(npcA, lineage.origin().originNpcId());
        assertEquals(sourceId, lineage.origin().originSemanticEntryId());
        assertEquals(2, lineage.hops().size());
        assertEquals(npcB, lineage.hops().get(1).speakerNpcId());
        assertEquals(npcC, lineage.hops().get(1).listenerNpcId());

        assertEquals(abEvidence.knowledgeTransferTransformation(), bcEvidence.knowledgeTransferTransformation());
        assertEquals(1, bcEvidence.knowledgeTransferTransformation().steps().size());
        SemanticMemoryEntry cBelief = SemanticMemoryStore.forWorld(world).getRecent(npcC, 64).getFirst();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, cBelief.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, cBelief.provenance());
        assertEquals("The western road is flooded.", cBelief.statement());
        assertEquals(List.of(player), cBelief.relatedEntities());
    }

    @Test
    void settlementDeliveredOppositionUsesExistingTruthNeutralContradictionProducer() {
        Path world = tempDir.resolve("contradiction");
        UUID speaker = id(20);
        UUID listener = id(21);
        UUID player = id(93);

        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                id(200),
                speaker,
                SemanticMemoryEntry.Kind.BELIEF,
                "The gate is not open",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                100L,
                0L,
                55,
                60,
                List.of(id(201))
        ), 64);
        ControlledSemanticMemoryIngestor.recordFact(
                world,
                observed(id(202), listener, player, "The gate is open", 200L),
                64
        );

        SettlementKnowledgeFlowLifecycle.CycleResult result = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 5, 2_400L, List.of(speaker, listener), 64, 64);

        assertEquals(1, result.successfulTransfers());
        List<SemanticContradictionHistory.ResolvedSemanticContradiction> history =
                SemanticContradictionHistory.load(world, listener, player, 8);
        assertEquals(1, history.size());

        List<SemanticMemoryEntry> listenerMemory = SemanticMemoryStore.forWorld(world).getRecent(listener, 64);
        SemanticMemoryEntry fact = listenerMemory.stream()
                .filter(entry -> entry.kind() == SemanticMemoryEntry.Kind.FACT)
                .findFirst().orElseThrow();
        SemanticMemoryEntry belief = listenerMemory.stream()
                .filter(entry -> entry.kind() == SemanticMemoryEntry.Kind.BELIEF)
                .findFirst().orElseThrow();
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, fact.provenance());
        assertEquals(100, fact.confidence());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, belief.provenance());
        assertEquals(50, belief.confidence());
    }

    private static MemoryEvent observed(UUID eventId, UUID npc, UUID player, String summary, long gameTime) {
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

    private static SemanticMemoryEntry fact(
            UUID entryId,
            UUID npc,
            String statement,
            List<UUID> scope,
            long gameTime
    ) {
        return new SemanticMemoryEntry(
                entryId,
                npc,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                scope,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                90,
                100,
                List.of(id(700_000 + (int) gameTime))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
