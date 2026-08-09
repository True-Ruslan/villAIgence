package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeTransferProvenanceResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesNewestRetainedValidDirectBranchWithoutListenerInput() {
        Path world = tempDir.resolve("resolver");
        UUID speaker = id(1);
        UUID originA = id(2);
        UUID originX = id(3);
        UUID sourceA = id(101);
        UUID sourceX = id(102);
        UUID speakerBeliefId = id(201);

        MemoryEvent older = firstHopEvidence(originA, speaker, sourceA, "Bridge destroyed", 100L);
        MemoryEvent newer = firstHopEvidence(originX, speaker, sourceX, "Bridge destroyed", 200L);
        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        eventStore.append(older, 64);
        eventStore.append(newer, 64);

        SemanticMemoryEntry speakerBelief = new SemanticMemoryEntry(
                speakerBeliefId,
                speaker,
                SemanticMemoryEntry.Kind.BELIEF,
                "Bridge destroyed",
                List.of(),
                MemoryEvent.Provenance.NPC_TOLD,
                200L,
                0L,
                50,
                50,
                List.of(older.id(), newer.id())
        );

        KnowledgeTransferProvenanceResolver.ResolvedSource resolved =
                KnowledgeTransferProvenanceResolver.resolve(eventStore, speakerBelief).orElseThrow();

        assertEquals(newer.id(), resolved.evidence().id());
        assertEquals(newer.knowledgeTransferProvenance(), resolved.provenance());
    }

    private static MemoryEvent firstHopEvidence(
            UUID origin,
            UUID listener,
            UUID sourceId,
            String statement,
            long gameTime
    ) {
        SemanticMemoryEntry source = new SemanticMemoryEntry(
                sourceId,
                origin,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime - 1L,
                0L,
                100,
                100,
                List.of(id(900 + (int) gameTime))
        );
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                origin, listener, sourceId, gameTime);
        KnowledgeTransferProvenance provenance = KnowledgeTransferProvenanceFactory.firstHop(
                source, listener, evidenceId, gameTime).orElseThrow();
        return NpcToldDialogueAdapter.create(
                origin, listener, sourceId, gameTime, statement, provenance).orElseThrow();
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
