package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeTransferRumorPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void exactTwoHopLineageSurvivesFreshRootAndReplayIsByteIdempotent() throws Exception {
        Path sourceWorld = tempDir.resolve("multi-hop-source");
        Path reloadedWorld = tempDir.resolve("multi-hop-reloaded");
        UUID a = id(1);
        UUID b = id(2);
        UUID c = id(3);
        UUID player = id(90);
        UUID originEntry = id(101);
        seedFact(sourceWorld, a, originEntry, "The northern bridge is closed", List.of(player));

        NpcKnowledgeTransferResult ab = transfer(sourceWorld, a, b, originEntry, 100L);
        NpcKnowledgeTransferResult bc = transfer(sourceWorld, b, c, ab.semanticEntryId(), 200L);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, bc.status());
        MemoryEvent sourceEvidence = MemoryEventStore.forWorld(sourceWorld)
                .findById(c, bc.evidenceEventId()).orElseThrow();
        KnowledgeTransferProvenance sourceLineage = sourceEvidence.knowledgeTransferProvenance();
        assertEquals(2, sourceLineage.hops().size());

        copyStores(sourceWorld, reloadedWorld);
        MemoryEvent reloadedEvidence = MemoryEventStore.forWorld(reloadedWorld)
                .findById(c, bc.evidenceEventId()).orElseThrow();
        SemanticMemoryEntry reloadedBelief = SemanticMemoryStore.forWorld(reloadedWorld)
                .findById(c, bc.semanticEntryId()).orElseThrow();

        assertEquals(sourceEvidence, reloadedEvidence);
        assertEquals(sourceLineage, reloadedEvidence.knowledgeTransferProvenance());
        assertEquals(List.of(bc.evidenceEventId()), reloadedBelief.sourceEventIds());
        assertEquals(originEntry, sourceLineage.origin().originSemanticEntryId());
        assertEquals(ab.evidenceEventId(), sourceLineage.hops().get(0).evidenceEventId());
        assertEquals(bc.evidenceEventId(), sourceLineage.hops().get(1).evidenceEventId());

        String memoryBefore = Files.readString(reloadedWorld.resolve("livingworld/memory2.json"));
        String semanticBefore = Files.readString(reloadedWorld.resolve("livingworld/semantic-memory.json"));
        NpcKnowledgeTransferResult replay = transfer(reloadedWorld, b, c, ab.semanticEntryId(), 200L);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, replay.status());
        assertEquals(bc.evidenceEventId(), replay.evidenceEventId());
        assertEquals(bc.semanticEntryId(), replay.semanticEntryId());
        assertEquals(memoryBefore, Files.readString(reloadedWorld.resolve("livingworld/memory2.json")));
        assertEquals(semanticBefore, Files.readString(reloadedWorld.resolve("livingworld/semantic-memory.json")));
    }

    @Test
    void globalPrivateAndSharedScopeRemainExactAcrossTwoHopsAndRumorEvidenceIsNotPlayerDialogue() {
        UUID playerA = id(500);
        UUID playerB = id(501);
        UUID entityX = id(502);

        assertScopeAcrossTwoHops(
                tempDir.resolve("global"),
                1_000,
                "The public well is repaired",
                List.of(),
                playerA,
                playerB,
                true,
                true
        );
        assertScopeAcrossTwoHops(
                tempDir.resolve("private"),
                2_000,
                "Player A stores grain in the east barn",
                List.of(playerA),
                playerA,
                playerB,
                true,
                false
        );
        assertScopeAcrossTwoHops(
                tempDir.resolve("shared"),
                3_000,
                "Player A and the guild share the cellar",
                List.of(entityX, playerA),
                playerA,
                playerB,
                true,
                false
        );
    }

    private void assertScopeAcrossTwoHops(
            Path world,
            int base,
            String statement,
            List<UUID> scope,
            UUID playerA,
            UUID playerB,
            boolean visibleToA,
            boolean visibleToB
    ) {
        UUID a = id(base + 1);
        UUID b = id(base + 2);
        UUID c = id(base + 3);
        UUID originEntry = id(base + 4);
        seedFact(world, a, originEntry, statement, scope);

        NpcKnowledgeTransferResult ab = transfer(world, a, b, originEntry, 100L);
        NpcKnowledgeTransferResult bc = transfer(world, b, c, ab.semanticEntryId(), 200L);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, bc.status());

        SemanticMemoryEntry cBelief = SemanticMemoryStore.forWorld(world)
                .findById(c, bc.semanticEntryId()).orElseThrow();
        MemoryEvent cEvidence = MemoryEventStore.forWorld(world)
                .findById(c, bc.evidenceEventId()).orElseThrow();
        assertEquals(sorted(scope), sorted(cBelief.relatedEntities()));
        assertFalse(cBelief.relatedEntities().contains(a));
        assertFalse(cBelief.relatedEntities().contains(b));
        assertEquals(sorted(scope), cEvidence.knowledgeTransferProvenance().origin().relatedEntities());
        assertEquals(visibleToA, contextContains(world, c, playerA, statement));
        assertEquals(visibleToB, contextContains(world, c, playerB, statement));
        assertEquals(List.of(), Memory2DialogueHistory.load(world, c, playerA));

        Memory2DialogueIngestor.record(
                world,
                c,
                playerA,
                300L,
                "Do you remember our discussion?",
                "Yes, I remember it.",
                64,
                0L
        );
        assertEquals(List.of(
                new WorkingMemoryMessage("user", "Do you remember our discussion?"),
                new WorkingMemoryMessage("assistant", "Yes, I remember it.")
        ), Memory2DialogueHistory.load(world, c, playerA));
    }

    private static NpcKnowledgeTransferResult transfer(
            Path world,
            UUID speaker,
            UUID listener,
            UUID source,
            long gameTime
    ) {
        return NpcKnowledgeTransferLifecycle.transfer(world, speaker, listener, source, gameTime, 128, 128);
    }

    private static boolean contextContains(Path world, UUID npc, UUID player, String statement) {
        return SemanticMemoryContextProvider.load(world, npc, player, 1_000L)
                .stream().anyMatch(line -> line.contains(statement));
    }

    private static void seedFact(
            Path world,
            UUID owner,
            UUID entryId,
            String statement,
            List<UUID> scope
    ) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                entryId,
                owner,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                scope,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                90,
                100,
                List.of(UUID.nameUUIDFromBytes((entryId + "-origin").getBytes(StandardCharsets.UTF_8)))
        ), 128);
    }

    private static void copyStores(Path sourceWorld, Path targetWorld) throws Exception {
        Path targetLiving = targetWorld.resolve("livingworld");
        Files.createDirectories(targetLiving);
        Files.copy(
                sourceWorld.resolve("livingworld/memory2.json"),
                targetLiving.resolve("memory2.json"),
                StandardCopyOption.REPLACE_EXISTING
        );
        Files.copy(
                sourceWorld.resolve("livingworld/semantic-memory.json"),
                targetLiving.resolve("semantic-memory.json"),
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    private static List<UUID> sorted(List<UUID> values) {
        List<UUID> result = new ArrayList<>(values);
        result.sort(Comparator.comparing(UUID::toString));
        return List.copyOf(result);
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
