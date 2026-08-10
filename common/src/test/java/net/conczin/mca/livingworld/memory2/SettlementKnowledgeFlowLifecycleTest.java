package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementKnowledgeFlowLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void cycleDelegatesToExactNpcTransferAndCreatesOnlyListenerNpcToldBelief() {
        Path world = tempDir.resolve("delegation");
        UUID speaker = id(1);
        UUID listener = id(2);
        UUID player = id(90);
        UUID sourceId = id(100);
        UUID sourceEvidence = id(101);
        long cycleTime = 2_400L;

        SemanticMemoryEntry source = new SemanticMemoryEntry(
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                "The north bridge is destroyed",
                List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L,
                0L,
                91,
                100,
                List.of(sourceEvidence)
        );
        SemanticMemoryStore.forWorld(world).append(source, 64);

        SettlementKnowledgeFlowLifecycle.CycleResult result = SettlementKnowledgeFlowLifecycle.runCycle(
                world,
                7,
                cycleTime,
                List.of(speaker, listener),
                64,
                64
        );

        assertEquals(1, result.opportunities());
        assertEquals(1, result.attemptedTransfers());
        assertEquals(1, result.successfulTransfers());
        assertEquals(List.of(NpcKnowledgeTransferResult.Status.ADMITTED), result.statuses());

        List<SemanticMemoryEntry> sourceMemory = SemanticMemoryStore.forWorld(world).getRecent(speaker, 64);
        assertEquals(1, sourceMemory.size());
        assertEquals(SemanticMemoryEntry.Kind.FACT, sourceMemory.getFirst().kind());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, sourceMemory.getFirst().provenance());
        assertEquals(100, sourceMemory.getFirst().confidence());

        List<SemanticMemoryEntry> listenerMemory = SemanticMemoryStore.forWorld(world).getRecent(listener, 64);
        assertEquals(1, listenerMemory.size());
        SemanticMemoryEntry transferred = listenerMemory.getFirst();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, transferred.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, transferred.provenance());
        assertEquals("The north bridge is destroyed", transferred.statement());
        assertEquals(List.of(player), transferred.relatedEntities());
        assertEquals(50, transferred.importance());
        assertEquals(50, transferred.confidence());
        assertFalse(listenerMemory.stream().anyMatch(entry -> entry.kind() == SemanticMemoryEntry.Kind.FACT));

        MemoryEvent evidence = MemoryEventStore.forWorld(world).findById(
                listener,
                NpcToldDialogueAdapter.deterministicEvidenceId(speaker, listener, sourceId, cycleTime)
        ).orElseThrow();
        assertEquals(MemoryEvent.Type.DIALOGUE, evidence.type());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, evidence.provenance());
        assertEquals(List.of(listener, speaker), evidence.participants());
        assertEquals(List.of(player), transferred.relatedEntities());
    }

    @Test
    void exactSameCycleReplayCannotRetargetOrCreateSecondTransferEvidence() {
        Path world = tempDir.resolve("replay");
        UUID speaker = id(10);
        UUID listenerA = id(11);
        UUID listenerB = id(12);
        UUID listenerC = id(13);
        UUID sourceId = id(110);
        long cycleTime = 3_600L;

        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                "The well is contaminated",
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                200L,
                0L,
                90,
                100,
                List.of(id(111))
        ), 64);

        List<UUID> residents = List.of(speaker, listenerA, listenerB, listenerC);
        SettlementKnowledgeFlowLifecycle.CycleResult first = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 8, cycleTime, residents, 64, 64);
        assertEquals(1, first.successfulTransfers());

        long evidenceBefore = residents.stream()
                .flatMap(npc -> MemoryEventStore.forWorld(world).getRecent(npc, 64).stream())
                .filter(event -> event.type() == MemoryEvent.Type.DIALOGUE
                        && event.provenance() == MemoryEvent.Provenance.NPC_TOLD)
                .count();
        long listenerClaimsBefore = residents.stream()
                .filter(npc -> !npc.equals(speaker))
                .filter(npc -> SemanticMemoryStore.forWorld(world).getRecent(npc, 64).stream()
                        .anyMatch(entry -> SemanticMemoryIdentity.canonicalStatement(entry.statement())
                                .equals("the well is contaminated")))
                .count();

        SettlementKnowledgeFlowLifecycle.CycleResult replay = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 8, cycleTime, residents, 64, 64);

        long evidenceAfter = residents.stream()
                .flatMap(npc -> MemoryEventStore.forWorld(world).getRecent(npc, 64).stream())
                .filter(event -> event.type() == MemoryEvent.Type.DIALOGUE
                        && event.provenance() == MemoryEvent.Provenance.NPC_TOLD)
                .count();
        long listenerClaimsAfter = residents.stream()
                .filter(npc -> !npc.equals(speaker))
                .filter(npc -> SemanticMemoryStore.forWorld(world).getRecent(npc, 64).stream()
                        .anyMatch(entry -> SemanticMemoryIdentity.canonicalStatement(entry.statement())
                                .equals("the well is contaminated")))
                .count();

        assertEquals(evidenceBefore, evidenceAfter);
        assertEquals(listenerClaimsBefore, listenerClaimsAfter);
        assertEquals(1, listenerClaimsAfter);
        assertEquals(0, replay.successfulTransfers());
        assertTrue(replay.opportunities() <= 1);
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
