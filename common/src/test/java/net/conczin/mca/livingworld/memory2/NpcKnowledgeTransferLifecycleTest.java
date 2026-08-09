package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NpcKnowledgeTransferLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void transfersPersistedSpeakerFactAsListenerNpcToldBeliefWithExactEvidence() {
        Path world = tempDir.resolve("fact-transfer");
        UUID speaker = UUID.fromString("00000000-0000-0000-0000-000000030001");
        UUID listener = UUID.fromString("00000000-0000-0000-0000-000000030002");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000030003");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000030004");
        UUID sourceEvidenceId = UUID.fromString("00000000-0000-0000-0000-000000030005");
        long transferTime = 250L;

        SemanticMemoryEntry source = new SemanticMemoryEntry(
                sourceId, speaker, SemanticMemoryEntry.Kind.FACT,
                "The bridge is destroyed", List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 1_000L, 90, 100, List.of(sourceEvidenceId));
        SemanticMemoryStore.forWorld(world).append(source, 64);

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, transferTime, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status());
        UUID expectedEvidenceId = NpcToldDialogueAdapter.deterministicEvidenceId(
                speaker, listener, sourceId, transferTime);
        assertEquals(expectedEvidenceId, result.evidenceEventId());

        MemoryEvent evidence = MemoryEventStore.forWorld(world)
                .findById(listener, expectedEvidenceId).orElseThrow();
        assertEquals(listener, evidence.ownerNpcId());
        assertEquals(MemoryEvent.Type.DIALOGUE, evidence.type());
        assertEquals(List.of(listener, speaker), evidence.participants());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, evidence.provenance());
        assertEquals("NPC told: The bridge is destroyed", evidence.summary());
        assertEquals(transferTime, evidence.gameTime());
        assertEquals(0L, evidence.createdAtEpochMillis());
        assertEquals(50, evidence.importance());
        assertEquals(0, evidence.emotionalWeight());
        assertEquals(50, evidence.confidence());
        assertFirstHopOrigin(evidence, source, listener, expectedEvidenceId, transferTime);

        List<SemanticMemoryEntry> listenerMemory = SemanticMemoryStore.forWorld(world).getRecent(listener, 64);
        assertEquals(1, listenerMemory.size());
        SemanticMemoryEntry transferred = listenerMemory.getFirst();
        assertEquals(result.semanticEntryId(), transferred.id());
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, transferred.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, transferred.provenance());
        assertEquals("The bridge is destroyed", transferred.statement());
        assertEquals(List.of(player), transferred.relatedEntities());
        assertEquals(List.of(expectedEvidenceId), transferred.sourceEventIds());
        assertEquals(50, transferred.importance());
        assertEquals(50, transferred.confidence());
        assertFalse(listenerMemory.stream().anyMatch(entry -> entry.kind() == SemanticMemoryEntry.Kind.FACT));
    }

    @Test
    void transfersSpeakerPlayerToldBeliefAsListenerNpcToldWithoutUpstreamProvenanceOrSources() {
        Path world = tempDir.resolve("belief-transfer");
        UUID speaker = UUID.fromString("00000000-0000-0000-0000-000000031001");
        UUID listener = UUID.fromString("00000000-0000-0000-0000-000000031002");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000031003");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000031004");
        UUID upstreamEvidenceId = UUID.fromString("00000000-0000-0000-0000-000000031005");
        long transferTime = 400L;

        SemanticMemoryEntry source = new SemanticMemoryEntry(
                sourceId, speaker, SemanticMemoryEntry.Kind.BELIEF,
                "The mill is closed", List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                300L, 3_000L, 88, 77, List.of(upstreamEvidenceId));
        SemanticMemoryStore.forWorld(world).append(source, 64);

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, transferTime, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status());
        UUID transferEvidenceId = NpcToldDialogueAdapter.deterministicEvidenceId(
                speaker, listener, sourceId, transferTime);
        MemoryEvent evidence = MemoryEventStore.forWorld(world)
                .findById(listener, transferEvidenceId).orElseThrow();
        assertFirstHopOrigin(evidence, source, listener, transferEvidenceId, transferTime);

        SemanticMemoryEntry transferred = SemanticMemoryStore.forWorld(world).getRecent(listener, 64).getFirst();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, transferred.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, transferred.provenance());
        assertEquals(List.of(player), transferred.relatedEntities());
        assertEquals(List.of(transferEvidenceId), transferred.sourceEventIds());
        assertFalse(transferred.sourceEventIds().contains(upstreamEvidenceId));
        assertEquals(50, transferred.importance());
        assertEquals(50, transferred.confidence());
    }

    @Test
    void transfersSpeakerInferredBeliefAsFirstHopWithoutPromotingTruth() {
        Path world = tempDir.resolve("inferred-transfer");
        UUID speaker = UUID.fromString("00000000-0000-0000-0000-000000032001");
        UUID listener = UUID.fromString("00000000-0000-0000-0000-000000032002");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000032003");
        SemanticMemoryEntry source = new SemanticMemoryEntry(
                sourceId, speaker, SemanticMemoryEntry.Kind.BELIEF,
                "The road may be unsafe", List.of(), MemoryEvent.Provenance.INFERRED,
                500L, 0L, 60, 40,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000032004")));
        SemanticMemoryStore.forWorld(world).append(source, 64);

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, 600L, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status());
        MemoryEvent evidence = MemoryEventStore.forWorld(world)
                .findById(listener, result.evidenceEventId()).orElseThrow();
        assertFirstHopOrigin(evidence, source, listener, result.evidenceEventId(), 600L);
        SemanticMemoryEntry transferred = SemanticMemoryStore.forWorld(world)
                .getRecent(listener, 64).getFirst();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, transferred.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, transferred.provenance());
    }

    @Test
    void inheritsExactLineageAcrossSecondNpcHopAndKeepsOnlyDirectSemanticEvidence() {
        Path world = tempDir.resolve("multi-hop-transfer");
        UUID npcA = UUID.fromString("00000000-0000-0000-0000-000000033001");
        UUID npcB = UUID.fromString("00000000-0000-0000-0000-000000033002");
        UUID npcC = UUID.fromString("00000000-0000-0000-0000-000000033003");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000033004");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000033005");
        SemanticMemoryEntry source = new SemanticMemoryEntry(
                sourceId, npcA, SemanticMemoryEntry.Kind.FACT,
                "The bell tower is damaged", List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L, 0L, 90, 100,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000033006")));
        SemanticMemoryStore.forWorld(world).append(source, 64);

        NpcKnowledgeTransferResult ab = NpcKnowledgeTransferLifecycle.transfer(
                world, npcA, npcB, sourceId, 200L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, ab.status());

        NpcKnowledgeTransferResult bc = NpcKnowledgeTransferLifecycle.transfer(
                world, npcB, npcC, ab.semanticEntryId(), 300L, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, bc.status());
        MemoryEvent bcEvidence = MemoryEventStore.forWorld(world)
                .findById(npcC, bc.evidenceEventId()).orElseThrow();
        KnowledgeTransferProvenance lineage = bcEvidence.knowledgeTransferProvenance();
        assertNotNull(lineage);
        assertEquals(npcA, lineage.origin().originNpcId());
        assertEquals(sourceId, lineage.origin().originSemanticEntryId());
        assertEquals(SemanticMemoryEntry.Kind.FACT, lineage.origin().originKind());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, lineage.origin().originProvenance());
        assertEquals("The bell tower is damaged", lineage.origin().statement());
        assertEquals(List.of(player), lineage.origin().relatedEntities());
        assertEquals(2, lineage.hops().size());

        KnowledgeTransferProvenance.Hop first = lineage.hops().get(0);
        assertEquals(npcA, first.speakerNpcId());
        assertEquals(npcB, first.listenerNpcId());
        assertEquals(sourceId, first.speakerSemanticEntryId());
        assertEquals(ab.evidenceEventId(), first.evidenceEventId());
        assertEquals(200L, first.gameTime());

        KnowledgeTransferProvenance.Hop second = lineage.hops().get(1);
        assertEquals(npcB, second.speakerNpcId());
        assertEquals(npcC, second.listenerNpcId());
        assertEquals(ab.semanticEntryId(), second.speakerSemanticEntryId());
        assertEquals(bc.evidenceEventId(), second.evidenceEventId());
        assertEquals(300L, second.gameTime());

        SemanticMemoryEntry cBelief = SemanticMemoryStore.forWorld(world)
                .findById(npcC, bc.semanticEntryId()).orElseThrow();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, cBelief.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, cBelief.provenance());
        assertEquals(List.of(player), cBelief.relatedEntities());
        assertEquals(List.of(bc.evidenceEventId()), cBelief.sourceEventIds());
        assertFalse(cBelief.sourceEventIds().contains(ab.evidenceEventId()));
    }

    private static void assertFirstHopOrigin(
            MemoryEvent evidence,
            SemanticMemoryEntry source,
            UUID listener,
            UUID evidenceId,
            long gameTime
    ) {
        KnowledgeTransferProvenance provenance = evidence.knowledgeTransferProvenance();
        assertNotNull(provenance);
        assertEquals(source.ownerNpcId(), provenance.origin().originNpcId());
        assertEquals(source.id(), provenance.origin().originSemanticEntryId());
        assertEquals(source.kind(), provenance.origin().originKind());
        assertEquals(source.provenance(), provenance.origin().originProvenance());
        assertEquals(SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(source.statement()),
                provenance.origin().statement());
        assertEquals(KnowledgeTransferProvenancePolicy.canonicalIds(source.relatedEntities()),
                provenance.origin().relatedEntities());
        assertEquals(1, provenance.hops().size());
        KnowledgeTransferProvenance.Hop hop = provenance.hops().getFirst();
        assertEquals(source.ownerNpcId(), hop.speakerNpcId());
        assertEquals(listener, hop.listenerNpcId());
        assertEquals(source.id(), hop.speakerSemanticEntryId());
        assertEquals(evidenceId, hop.evidenceEventId());
        assertEquals(gameTime, hop.gameTime());
    }
}
