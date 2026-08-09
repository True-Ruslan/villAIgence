package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeTransferTransformationPreservationTest {
    @TempDir
    Path tempDir;

    @Test
    void freshRootReplayPreservesTransformBudgetAndAllowsOnlyUnchangedPropagation() throws Exception {
        Path sourceWorld = tempDir.resolve("source");
        Path reloadedWorld = tempDir.resolve("reloaded");
        UUID a = id(1);
        UUID b = id(2);
        UUID c = id(3);
        UUID d = id(4);
        UUID sourceId = id(100);
        String originStatement = "The western gate is closed. Repairs finish tomorrow.";
        seedFact(sourceWorld, a, sourceId, originStatement);

        NpcKnowledgeTransferResult ab = NpcKnowledgeTransferLifecycle.transfer(
                sourceWorld, a, b, sourceId, 100L, 64, 64);
        NpcKnowledgeTransferResult bc = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                sourceWorld, b, c, ab.semanticEntryId(), 200L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, ab.status());
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, bc.status());

        MemoryEvent sourceTransformedEvidence = MemoryEventStore.forWorld(sourceWorld)
                .findById(c, bc.evidenceEventId()).orElseThrow();
        KnowledgeTransferTransformation sourceTransformation =
                sourceTransformedEvidence.knowledgeTransferTransformation();
        assertEquals(1, sourceTransformation.transformationsUsed());
        assertEquals(originStatement, sourceTransformedEvidence.knowledgeTransferProvenance().origin().statement());
        assertEquals("The western gate is closed.", sourceTransformation.currentStatement());

        copyStores(sourceWorld, reloadedWorld);

        NpcKnowledgeTransferResult replay = NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                reloadedWorld, b, c, ab.semanticEntryId(), 200L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, replay.status());
        assertEquals(bc.evidenceEventId(), replay.evidenceEventId());
        assertEquals(bc.semanticEntryId(), replay.semanticEntryId());
        assertEquals(sourceTransformedEvidence, MemoryEventStore.forWorld(reloadedWorld)
                .findById(c, replay.evidenceEventId()).orElseThrow());

        NpcKnowledgeTransferResult secondTransform =
                NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                        reloadedWorld, c, d, bc.semanticEntryId(), 300L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.TRANSFORMATION_LIMIT_REACHED,
                secondTransform.status());
        assertTrue(MemoryEventStore.forWorld(reloadedWorld).getRecent(d, 64).isEmpty());
        assertTrue(SemanticMemoryStore.forWorld(reloadedWorld).getRecent(d, 64).isEmpty());

        NpcKnowledgeTransferResult unchanged = NpcKnowledgeTransferLifecycle.transfer(
                reloadedWorld, c, d, bc.semanticEntryId(), 300L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, unchanged.status());

        MemoryEvent dEvidence = MemoryEventStore.forWorld(reloadedWorld)
                .findById(d, unchanged.evidenceEventId()).orElseThrow();
        assertEquals(sourceTransformation, dEvidence.knowledgeTransferTransformation());
        assertEquals(originStatement, dEvidence.knowledgeTransferProvenance().origin().statement());
        assertEquals(3, dEvidence.knowledgeTransferProvenance().hops().size());

        SemanticMemoryEntry dRumor = SemanticMemoryStore.forWorld(reloadedWorld)
                .findById(d, unchanged.semanticEntryId()).orElseThrow();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, dRumor.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, dRumor.provenance());
        assertEquals("The western gate is closed.", dRumor.statement());
        assertEquals(50, dRumor.confidence());
        assertFalse(SemanticMemoryStore.forWorld(reloadedWorld).getRecent(d, 64).stream()
                .anyMatch(entry -> entry.kind() == SemanticMemoryEntry.Kind.FACT));
    }

    @Test
    void forgottenTransformedDirectEvidenceCannotBeReconstructedFromSurvivingProse() {
        Path world = tempDir.resolve("forgotten");
        UUID a = id(10);
        UUID b = id(11);
        UUID c = id(12);
        UUID sourceId = id(110);
        seedFact(world, a, sourceId,
                "The northern road is blocked. A tree fell overnight.");

        NpcKnowledgeTransferResult transformed =
                NpcKnowledgeTransferLifecycle.transferOmittingTrailingSentence(
                        world, a, b, sourceId, 100L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, transformed.status());
        SemanticMemoryEntry survivingRumor = SemanticMemoryStore.forWorld(world)
                .findById(b, transformed.semanticEntryId()).orElseThrow();
        assertEquals("The northern road is blocked.", survivingRumor.statement());

        MemoryEventStore.forWorld(world).append(strongObservedEvent(b, 1_000L), 1);
        assertTrue(MemoryEventStore.forWorld(world)
                .findById(b, transformed.evidenceEventId()).isEmpty());

        RumorFallibilityState fallibility = RumorFallibilityResolver.resolve(
                MemoryEventStore.forWorld(world), survivingRumor).orElseThrow();
        assertEquals(RumorFallibilityState.SourcePath.UNRESOLVED, fallibility.sourcePath());
        assertEquals(RumorFallibilityState.UNKNOWN_TRANSFORMATIONS,
                fallibility.transformationsUsed());

        NpcKnowledgeTransferResult attempted = NpcKnowledgeTransferLifecycle.transfer(
                world, b, c, survivingRumor.id(), 200L, 64, 64);
        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_UNAVAILABLE, attempted.status());
        assertTrue(MemoryEventStore.forWorld(world).getRecent(c, 64).isEmpty());
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(c, 64).isEmpty());
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
                List.of(UUID.nameUUIDFromBytes(
                        (sourceId + "-origin").getBytes(StandardCharsets.UTF_8)))
        ), 64);
    }

    private static MemoryEvent strongObservedEvent(UUID owner, long gameTime) {
        return new MemoryEvent(
                UUID.nameUUIDFromBytes(
                        (owner + "-strong-" + gameTime).getBytes(StandardCharsets.UTF_8)),
                owner,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                "Strong server-observed event",
                List.of(owner),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                100,
                100,
                100,
                List.of()
        );
    }

    private static void copyStores(Path sourceWorld, Path targetWorld) throws Exception {
        Path target = targetWorld.resolve("livingworld");
        Files.createDirectories(target);
        Files.copy(sourceWorld.resolve("livingworld/memory2.json"), target.resolve("memory2.json"),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sourceWorld.resolve("livingworld/semantic-memory.json"),
                target.resolve("semantic-memory.json"), StandardCopyOption.REPLACE_EXISTING);
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
