package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeTransferTransformationLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void firstHopCanApplyOneDeterministicTrailingSentenceOmission() {
        Path world = tempDir.resolve("first-transform");
        UUID speaker = id(1);
        UUID listener = id(2);
        UUID sourceId = id(3);
        seedFact(world, speaker, sourceId,
                "The bridge is closed. Repairs finish tomorrow.");

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                world, speaker, listener, sourceId, 100L, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status());
        MemoryEvent evidence = MemoryEventStore.forWorld(world)
                .findById(listener, result.evidenceEventId()).orElseThrow();
        assertEquals("NPC told: The bridge is closed.", evidence.summary());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, evidence.provenance());
        assertEquals(50, evidence.confidence());

        KnowledgeTransferProvenance provenance = evidence.knowledgeTransferProvenance();
        assertNotNull(provenance);
        assertEquals("The bridge is closed. Repairs finish tomorrow.", provenance.origin().statement());
        assertEquals(1, provenance.hops().size());

        KnowledgeTransferTransformation transformation = evidence.knowledgeTransferTransformation();
        assertNotNull(transformation);
        assertEquals(1, transformation.transformationsUsed());
        assertEquals("The bridge is closed.", transformation.currentStatement());
        KnowledgeTransferTransformation.Step step = transformation.steps().getFirst();
        assertEquals(KnowledgeTransferTransformation.Kind.OMIT_TRAILING_SENTENCE, step.kind());
        assertEquals("The bridge is closed. Repairs finish tomorrow.", step.sourceStatement());
        assertEquals("The bridge is closed.", step.transformedStatement());
        assertEquals(speaker, step.speakerNpcId());
        assertEquals(listener, step.listenerNpcId());
        assertEquals(sourceId, step.sourceSemanticEntryId());
        assertEquals(result.evidenceEventId(), step.evidenceEventId());
        assertEquals(100L, step.gameTime());

        SemanticMemoryEntry retained = SemanticMemoryStore.forWorld(world)
                .findById(listener, result.semanticEntryId()).orElseThrow();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, retained.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, retained.provenance());
        assertEquals("The bridge is closed.", retained.statement());
        assertEquals(50, retained.confidence());
        assertFalse(SemanticMemoryStore.forWorld(world).getRecent(listener, 64).stream()
                .anyMatch(entry -> entry.kind() == SemanticMemoryEntry.Kind.FACT));
    }

    @Test
    void transformCanOccurOnLaterUntransformedHopAndThenPropagatesUnchanged() {
        Path world = tempDir.resolve("later-transform");
        UUID a = id(10);
        UUID b = id(11);
        UUID c = id(12);
        UUID d = id(13);
        UUID sourceId = id(14);
        seedFact(world, a, sourceId,
                "The orchard gate is locked. Mira has the key.");

        NpcKnowledgeTransferResult ab = NpcKnowledgeTransferLifecycle.transfer(
                world, a, b, sourceId, 100L, 64, 64);
        NpcKnowledgeTransferResult bc = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                world, b, c, ab.semanticEntryId(), 200L, 64, 64);
        NpcKnowledgeTransferResult cd = NpcKnowledgeTransferLifecycle.transfer(
                world, c, d, bc.semanticEntryId(), 300L, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, ab.status());
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, bc.status());
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, cd.status());

        MemoryEvent bcEvidence = MemoryEventStore.forWorld(world)
                .findById(c, bc.evidenceEventId()).orElseThrow();
        KnowledgeTransferTransformation transformation = bcEvidence.knowledgeTransferTransformation();
        assertNotNull(transformation);
        assertEquals("The orchard gate is locked.", transformation.currentStatement());

        MemoryEvent cdEvidence = MemoryEventStore.forWorld(world)
                .findById(d, cd.evidenceEventId()).orElseThrow();
        assertEquals(transformation, cdEvidence.knowledgeTransferTransformation());
        assertEquals("The orchard gate is locked. Mira has the key.",
                cdEvidence.knowledgeTransferProvenance().origin().statement());
        assertEquals(3, cdEvidence.knowledgeTransferProvenance().hops().size());

        SemanticMemoryEntry dClaim = SemanticMemoryStore.forWorld(world)
                .findById(d, cd.semanticEntryId()).orElseThrow();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, dClaim.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, dClaim.provenance());
        assertEquals("The orchard gate is locked.", dClaim.statement());
    }

    @Test
    void secondTransformationRequestIsRejectedAtLineageBudget() {
        Path world = tempDir.resolve("budget");
        UUID a = id(20);
        UUID b = id(21);
        UUID c = id(22);
        UUID sourceId = id(23);
        seedFact(world, a, sourceId,
                "The tower bell rang. The guard changed shift.");

        NpcKnowledgeTransferResult ab = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                world, a, b, sourceId, 100L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, ab.status());

        NpcKnowledgeTransferResult bc = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                world, b, c, ab.semanticEntryId(), 200L, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.TRANSFORMATION_LIMIT_REACHED, bc.status());
        assertNull(bc.evidenceEventId());
        assertNull(bc.semanticEntryId());
        assertTrue(MemoryEventStore.forWorld(world).getRecent(c, 64).isEmpty());
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(c, 64).isEmpty());
    }

    @Test
    void singleSentenceSourceReportsTransformationNotApplicableWithoutWriting() {
        Path world = tempDir.resolve("not-applicable");
        UUID speaker = id(30);
        UUID listener = id(31);
        UUID sourceId = id(32);
        seedFact(world, speaker, sourceId, "The mill is closed");

        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                world, speaker, listener, sourceId, 100L, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.TRANSFORMATION_NOT_APPLICABLE, result.status());
        assertNull(result.evidenceEventId());
        assertNull(result.semanticEntryId());
        assertTrue(MemoryEventStore.forWorld(world).getRecent(listener, 64).isEmpty());
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(listener, 64).isEmpty());
    }

    @Test
    void exactTransformedReplayIsIdempotent() {
        Path world = tempDir.resolve("replay");
        UUID speaker = id(40);
        UUID listener = id(41);
        UUID sourceId = id(42);
        seedFact(world, speaker, sourceId,
                "The south road is blocked. A tree fell overnight.");

        NpcKnowledgeTransferResult first = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                world, speaker, listener, sourceId, 100L, 64, 64);
        MemoryEvent firstEvidence = MemoryEventStore.forWorld(world)
                .findById(listener, first.evidenceEventId()).orElseThrow();
        SemanticMemoryEntry firstSemantic = SemanticMemoryStore.forWorld(world)
                .findById(listener, first.semanticEntryId()).orElseThrow();

        NpcKnowledgeTransferResult replay = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                world, speaker, listener, sourceId, 100L, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, replay.status());
        assertEquals(first.evidenceEventId(), replay.evidenceEventId());
        assertEquals(first.semanticEntryId(), replay.semanticEntryId());
        assertEquals(firstEvidence, MemoryEventStore.forWorld(world)
                .findById(listener, replay.evidenceEventId()).orElseThrow());
        assertEquals(firstSemantic, SemanticMemoryStore.forWorld(world)
                .findById(listener, replay.semanticEntryId()).orElseThrow());
        assertEquals(1, MemoryEventStore.forWorld(world).getRecent(listener, 64).size());
        assertEquals(1, SemanticMemoryStore.forWorld(world).getRecent(listener, 64).size());
    }

    @Test
    void sameIdentityConflictCannotReplaceExistingOrdinaryEvidence() {
        Path world = tempDir.resolve("conflict");
        UUID speaker = id(50);
        UUID listener = id(51);
        UUID sourceId = id(52);
        seedFact(world, speaker, sourceId,
                "The market opens at dawn. Traders arrive early.");

        NpcKnowledgeTransferResult ordinary = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, 100L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, ordinary.status());
        MemoryEvent original = MemoryEventStore.forWorld(world)
                .findById(listener, ordinary.evidenceEventId()).orElseThrow();
        assertNull(original.knowledgeTransferTransformation());

        NpcKnowledgeTransferResult conflicting = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                world, speaker, listener, sourceId, 100L, 64, 64);

        assertEquals(NpcKnowledgeTransferResult.Status.REJECTED, conflicting.status());
        assertEquals(ordinary.evidenceEventId(), conflicting.evidenceEventId());
        assertNull(conflicting.semanticEntryId());
        assertEquals(original, MemoryEventStore.forWorld(world)
                .findById(listener, ordinary.evidenceEventId()).orElseThrow());
        assertEquals(1, MemoryEventStore.forWorld(world).getRecent(listener, 64).size());
    }

    private static void seedFact(Path world, UUID owner, UUID sourceId, String statement) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                owner,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                90,
                100,
                List.of(UUID.nameUUIDFromBytes((sourceId + "-origin").getBytes(StandardCharsets.UTF_8)))
        ), 64);
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
