package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeTransferTransformationPolicyTest {
    @Test
    void omissionRemovesExactlyOneTrailingSentenceWithoutInventingText() {
        assertEquals(
                Optional.of("The bridge is closed."),
                KnowledgeTransferTransformationPolicy.omitTrailingSentence(
                        "  The bridge is closed.   Repairs finish tomorrow.  "
                )
        );
        assertEquals(
                Optional.of("A. B!"),
                KnowledgeTransferTransformationPolicy.omitTrailingSentence("A. B! C?")
        );
        assertEquals(Optional.empty(),
                KnowledgeTransferTransformationPolicy.omitTrailingSentence("The bridge is closed"));
        assertEquals(Optional.empty(),
                KnowledgeTransferTransformationPolicy.omitTrailingSentence("The bridge is closed."));
    }

    @Test
    void transformationSnapshotIsHardBoundedToOneStep() {
        UUID speaker = id(1);
        UUID listener = id(2);
        UUID sourceEntry = id(3);
        UUID evidence = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                speaker, listener, sourceEntry, 100L);
        KnowledgeTransferTransformation.Step step = new KnowledgeTransferTransformation.Step(
                KnowledgeTransferTransformation.Kind.OMIT_TRAILING_SENTENCE,
                "The bridge is closed. Repairs finish tomorrow.",
                "The bridge is closed.",
                speaker,
                listener,
                sourceEntry,
                evidence,
                100L
        );

        KnowledgeTransferTransformation one = new KnowledgeTransferTransformation(List.of(step));
        assertEquals(1, one.transformationsUsed());
        assertEquals("The bridge is closed.", one.currentStatement());
        assertEquals(1, KnowledgeTransferTransformationPolicy.MAX_TRANSFORMATIONS);

        assertThrows(IllegalArgumentException.class,
                () -> new KnowledgeTransferTransformation(List.of(step, step)));
    }

    @Test
    void validationBindsExactDeterministicStepToExistingProvenanceHop() {
        UUID speaker = id(10);
        UUID listener = id(11);
        UUID sourceEntry = id(12);
        UUID evidence = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                speaker, listener, sourceEntry, 500L);
        KnowledgeTransferProvenance provenance = provenance(
                speaker, listener, sourceEntry, evidence, 500L,
                "The orchard gate is locked. The key is with Mira."
        );

        KnowledgeTransferTransformation valid = transformation(
                speaker, listener, sourceEntry, evidence, 500L,
                "The orchard gate is locked. The key is with Mira.",
                "The orchard gate is locked."
        );
        assertTrue(KnowledgeTransferTransformationPolicy.valid(valid, provenance));
        assertTrue(KnowledgeTransferTransformationPolicy.matchesCurrentStatement(
                provenance, valid, "The orchard gate is locked."));
        assertFalse(KnowledgeTransferTransformationPolicy.matchesCurrentStatement(
                provenance, valid, "The orchard gate is open."));

        KnowledgeTransferTransformation wrongEvidence = transformation(
                speaker, listener, sourceEntry, id(999), 500L,
                "The orchard gate is locked. The key is with Mira.",
                "The orchard gate is locked."
        );
        assertFalse(KnowledgeTransferTransformationPolicy.valid(wrongEvidence, provenance));

        KnowledgeTransferTransformation fabricated = transformation(
                speaker, listener, sourceEntry, evidence, 500L,
                "The orchard gate is locked. The key is with Mira.",
                "The orchard gate is open."
        );
        assertFalse(KnowledgeTransferTransformationPolicy.valid(fabricated, provenance));
    }

    @Test
    void noTransformationPathStillMatchesOriginExactly() {
        UUID speaker = id(20);
        UUID listener = id(21);
        UUID sourceEntry = id(22);
        UUID evidence = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                speaker, listener, sourceEntry, 700L);
        KnowledgeTransferProvenance provenance = provenance(
                speaker, listener, sourceEntry, evidence, 700L,
                "The mill closes at sunset"
        );

        assertTrue(KnowledgeTransferTransformationPolicy.matchesCurrentStatement(
                provenance, null, "The mill closes at sunset"));
        assertFalse(KnowledgeTransferTransformationPolicy.matchesCurrentStatement(
                provenance, null, "The mill closes at dawn"));
    }

    private static KnowledgeTransferTransformation transformation(
            UUID speaker,
            UUID listener,
            UUID sourceEntry,
            UUID evidence,
            long gameTime,
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
                gameTime
        )));
    }

    private static KnowledgeTransferProvenance provenance(
            UUID speaker,
            UUID listener,
            UUID sourceEntry,
            UUID evidence,
            long gameTime,
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
                        speaker,
                        listener,
                        sourceEntry,
                        evidence,
                        gameTime
                ))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
