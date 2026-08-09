package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeTransferTransformationEvidenceTest {
    @TempDir
    Path tempDir;

    @Test
    void transformedNpcToldEvidencePersistsExactStructuredSnapshotAcrossFreshRoot() throws Exception {
        Path sourceWorld = tempDir.resolve("source");
        Path reloadedWorld = tempDir.resolve("reloaded");
        UUID speaker = id(1);
        UUID listener = id(2);
        UUID sourceEntry = id(3);
        long gameTime = 100L;
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                speaker, listener, sourceEntry, gameTime);
        KnowledgeTransferProvenance provenance = provenance(
                speaker, listener, sourceEntry, evidenceId, gameTime,
                "The bridge is closed. Repairs finish tomorrow."
        );
        KnowledgeTransferTransformation transformation = transformation(
                speaker, listener, sourceEntry, evidenceId, gameTime,
                "The bridge is closed. Repairs finish tomorrow.",
                "The bridge is closed."
        );

        MemoryEvent event = NpcToldDialogueAdapter.create(
                speaker,
                listener,
                sourceEntry,
                gameTime,
                "The bridge is closed.",
                provenance,
                transformation
        ).orElseThrow();
        assertEquals(transformation, event.knowledgeTransferTransformation());
        assertEquals(provenance, event.knowledgeTransferProvenance());

        MemoryEventStore.forWorld(sourceWorld).append(event, 64);
        Path target = reloadedWorld.resolve("livingworld");
        Files.createDirectories(target);
        Files.copy(
                sourceWorld.resolve("livingworld/memory2.json"),
                target.resolve("memory2.json"),
                StandardCopyOption.REPLACE_EXISTING
        );

        MemoryEvent reloaded = MemoryEventStore.forWorld(reloadedWorld)
                .findById(listener, evidenceId)
                .orElseThrow();
        assertEquals(event, reloaded);
        assertEquals(transformation, reloaded.knowledgeTransferTransformation());
    }

    @Test
    void ordinaryTransferEvidenceRemainsTransformationFreeAndByteCompatibleInMeaning() {
        UUID speaker = id(10);
        UUID listener = id(11);
        UUID sourceEntry = id(12);
        long gameTime = 200L;
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                speaker, listener, sourceEntry, gameTime);
        KnowledgeTransferProvenance provenance = provenance(
                speaker, listener, sourceEntry, evidenceId, gameTime,
                "The mill closes at sunset"
        );

        MemoryEvent event = NpcToldDialogueAdapter.create(
                speaker,
                listener,
                sourceEntry,
                gameTime,
                "The mill closes at sunset",
                provenance
        ).orElseThrow();

        assertNull(event.knowledgeTransferTransformation());
        assertTrue(NpcKnowledgeTransferPolicy.validEvidence(
                event,
                speaker,
                listener,
                sourceEntry,
                gameTime,
                "The mill closes at sunset",
                provenance,
                null
        ));
    }

    @Test
    void transformedEvidenceRejectsFabricatedOrMismatchedTransformationPayload() {
        UUID speaker = id(20);
        UUID listener = id(21);
        UUID sourceEntry = id(22);
        long gameTime = 300L;
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                speaker, listener, sourceEntry, gameTime);
        KnowledgeTransferProvenance provenance = provenance(
                speaker, listener, sourceEntry, evidenceId, gameTime,
                "The orchard gate is locked. Mira has the key."
        );
        KnowledgeTransferTransformation valid = transformation(
                speaker, listener, sourceEntry, evidenceId, gameTime,
                "The orchard gate is locked. Mira has the key.",
                "The orchard gate is locked."
        );
        KnowledgeTransferTransformation fabricated = transformation(
                speaker, listener, sourceEntry, evidenceId, gameTime,
                "The orchard gate is locked. Mira has the key.",
                "The orchard gate is open."
        );

        assertTrue(NpcToldDialogueAdapter.create(
                speaker, listener, sourceEntry, gameTime,
                "The orchard gate is locked.", provenance, valid
        ).isPresent());
        assertFalse(NpcToldDialogueAdapter.create(
                speaker, listener, sourceEntry, gameTime,
                "The orchard gate is open.", provenance, fabricated
        ).isPresent());
        assertFalse(NpcToldDialogueAdapter.create(
                speaker, listener, sourceEntry, gameTime,
                "The orchard gate is locked.", provenance, null
        ).isPresent());
    }

    @Test
    void sameTransferIdentityDoesNotValidateAgainstConflictingTransformationMode() {
        UUID speaker = id(30);
        UUID listener = id(31);
        UUID sourceEntry = id(32);
        long gameTime = 400L;
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                speaker, listener, sourceEntry, gameTime);
        KnowledgeTransferProvenance provenance = provenance(
                speaker, listener, sourceEntry, evidenceId, gameTime,
                "The tower bell rang. The guard changed shift."
        );
        KnowledgeTransferTransformation transformation = transformation(
                speaker, listener, sourceEntry, evidenceId, gameTime,
                "The tower bell rang. The guard changed shift.",
                "The tower bell rang."
        );
        MemoryEvent transformed = NpcToldDialogueAdapter.create(
                speaker, listener, sourceEntry, gameTime,
                "The tower bell rang.", provenance, transformation
        ).orElseThrow();

        assertTrue(NpcKnowledgeTransferPolicy.validEvidence(
                transformed,
                speaker,
                listener,
                sourceEntry,
                gameTime,
                "The tower bell rang.",
                provenance,
                transformation
        ));
        assertFalse(NpcKnowledgeTransferPolicy.validEvidence(
                transformed,
                speaker,
                listener,
                sourceEntry,
                gameTime,
                "The tower bell rang. The guard changed shift.",
                provenance,
                null
        ));
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
