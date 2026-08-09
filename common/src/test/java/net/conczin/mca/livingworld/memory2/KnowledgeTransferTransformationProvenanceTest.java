package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeTransferTransformationProvenanceTest {
    @TempDir
    Path tempDir;

    @Test
    void transformedSemanticClaimResolvesCanonicalOriginAndExactTransformation() {
        Path world = tempDir.resolve("resolve");
        UUID originNpc = id(1);
        UUID listener = id(2);
        UUID originEntry = id(3);
        long time = 100L;
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                originNpc, listener, originEntry, time);
        KnowledgeTransferProvenance provenance = firstHop(
                originNpc, listener, originEntry, evidenceId, time,
                "The bridge is closed. Repairs finish tomorrow."
        );
        KnowledgeTransferTransformation transformation = transformation(
                originNpc, listener, originEntry, evidenceId, time,
                "The bridge is closed. Repairs finish tomorrow.",
                "The bridge is closed."
        );
        MemoryEvent evidence = NpcToldDialogueAdapter.create(
                originNpc, listener, originEntry, time,
                "The bridge is closed.", provenance, transformation
        ).orElseThrow();
        MemoryEventStore.forWorld(world).append(evidence, 64);
        SemanticMemoryEntry listenerClaim = npcTold(
                id(4), listener, "The bridge is closed.", evidenceId);

        KnowledgeTransferProvenanceResolver.ResolvedSource resolved =
                KnowledgeTransferProvenanceResolver.resolve(
                        MemoryEventStore.forWorld(world), listenerClaim
                ).orElseThrow();

        assertEquals(provenance, resolved.provenance());
        assertEquals(transformation, resolved.transformation());
        assertEquals(evidence, resolved.evidence());
    }

    @Test
    void transformedLineageAppendsNextHopWithoutRewritingOriginOrTransformation() {
        UUID originNpc = id(10);
        UUID middleNpc = id(11);
        UUID listenerNpc = id(12);
        UUID originEntry = id(13);
        UUID middleEntry = id(14);
        long firstTime = 200L;
        long secondTime = 300L;
        UUID firstEvidence = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                originNpc, middleNpc, originEntry, firstTime);
        KnowledgeTransferProvenance first = firstHop(
                originNpc, middleNpc, originEntry, firstEvidence, firstTime,
                "The orchard gate is locked. Mira has the key."
        );
        KnowledgeTransferTransformation transformation = transformation(
                originNpc, middleNpc, originEntry, firstEvidence, firstTime,
                "The orchard gate is locked. Mira has the key.",
                "The orchard gate is locked."
        );
        SemanticMemoryEntry middleClaim = npcTold(
                middleEntry, middleNpc, "The orchard gate is locked.", firstEvidence);
        UUID secondEvidence = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                middleNpc, listenerNpc, middleEntry, secondTime);

        KnowledgeTransferProvenance appended = KnowledgeTransferProvenanceFactory.appendHop(
                first,
                middleClaim,
                listenerNpc,
                secondEvidence,
                secondTime,
                transformation
        ).orElseThrow();

        assertEquals(2, appended.hops().size());
        assertEquals("The orchard gate is locked. Mira has the key.", appended.origin().statement());
        assertTrue(KnowledgeTransferTransformationPolicy.valid(transformation, appended));
        assertTrue(KnowledgeTransferTransformationPolicy.matchesCurrentStatement(
                appended, transformation, middleClaim.statement()));

        MemoryEvent downstream = NpcToldDialogueAdapter.create(
                middleNpc,
                listenerNpc,
                middleEntry,
                secondTime,
                middleClaim.statement(),
                appended,
                transformation
        ).orElseThrow();
        assertEquals(transformation, downstream.knowledgeTransferTransformation());
    }

    @Test
    void transformedClaimWithoutRetainedDirectEvidenceDoesNotReconstructProvenanceFromProse() {
        Path world = tempDir.resolve("missing");
        SemanticMemoryEntry unsupported = npcTold(
                id(30), id(31), "The bridge is closed.", id(999));

        assertTrue(KnowledgeTransferProvenanceResolver.resolve(
                MemoryEventStore.forWorld(world), unsupported).isEmpty());
    }

    private static SemanticMemoryEntry npcTold(
            UUID id,
            UUID owner,
            String statement,
            UUID sourceEvent
    ) {
        return new SemanticMemoryEntry(
                id,
                owner,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                List.of(),
                MemoryEvent.Provenance.NPC_TOLD,
                100L,
                0L,
                50,
                50,
                List.of(sourceEvent)
        );
    }

    private static KnowledgeTransferProvenance firstHop(
            UUID speaker,
            UUID listener,
            UUID sourceEntry,
            UUID evidence,
            long time,
            String statement
    ) {
        return new KnowledgeTransferProvenance(
                new KnowledgeTransferProvenance.Origin(
                        speaker,
                        sourceEntry,
                        SemanticMemoryEntry.Kind.FACT,
                        MemoryEvent.Provenance.SYSTEM_OBSERVED,
                        statement,
                        List.of()
                ),
                List.of(new KnowledgeTransferProvenance.Hop(
                        speaker, listener, sourceEntry, evidence, time
                ))
        );
    }

    private static KnowledgeTransferTransformation transformation(
            UUID speaker,
            UUID listener,
            UUID sourceEntry,
            UUID evidence,
            long time,
            String sourceStatement,
            String transformedStatement
    ) {
        return new KnowledgeTransferTransformation(List.of(new KnowledgeTransferTransformation.Step(
                KnowledgeTransferTransformation.Kind.OMIT_TRAILING_SENTENCE,
                sourceStatement,
                transformedStatement,
                speaker,
                listener,
                sourceEntry,
                evidence,
                time
        )));
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
