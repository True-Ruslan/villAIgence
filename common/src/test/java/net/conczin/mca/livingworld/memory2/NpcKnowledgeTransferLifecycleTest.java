package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                "The bridge is destroyed",
                List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L,
                1_000L,
                90,
                100,
                List.of(sourceEvidenceId)
        );
        SemanticMemoryStore.forWorld(world).append(source, 64);

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, transferTime, 64, 64
        );

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status());
        UUID expectedEvidenceId = NpcToldDialogueAdapter.deterministicEvidenceId(
                speaker, listener, sourceId, transferTime
        );
        assertEquals(expectedEvidenceId, result.evidenceEventId());

        MemoryEvent evidence = MemoryEventStore.forWorld(world)
                .findById(listener, expectedEvidenceId)
                .orElseThrow();
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
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.BELIEF,
                "The mill is closed",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                300L,
                3_000L,
                88,
                77,
                List.of(upstreamEvidenceId)
        );
        SemanticMemoryStore.forWorld(world).append(source, 64);

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, transferTime, 64, 64
        );

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status());
        UUID transferEvidenceId = NpcToldDialogueAdapter.deterministicEvidenceId(
                speaker, listener, sourceId, transferTime
        );
        List<SemanticMemoryEntry> listenerMemory = SemanticMemoryStore.forWorld(world).getRecent(listener, 64);
        assertEquals(1, listenerMemory.size());
        SemanticMemoryEntry transferred = listenerMemory.getFirst();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, transferred.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, transferred.provenance());
        assertEquals(List.of(player), transferred.relatedEntities());
        assertEquals(List.of(transferEvidenceId), transferred.sourceEventIds());
        assertFalse(transferred.sourceEventIds().contains(upstreamEvidenceId));
        assertEquals(50, transferred.importance());
        assertEquals(50, transferred.confidence());
    }
}
