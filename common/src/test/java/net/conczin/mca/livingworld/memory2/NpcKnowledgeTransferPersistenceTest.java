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

class NpcKnowledgeTransferPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void retainedTransferSurvivesFreshRootReloadAndExactReplayRemainsIdempotent() throws Exception {
        Path worldA = tempDir.resolve("reload-a");
        Path worldB = tempDir.resolve("reload-b");
        UUID speaker = UUID.fromString("00000000-0000-0000-0000-000000070001");
        UUID listener = UUID.fromString("00000000-0000-0000-0000-000000070002");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000070003");
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000070004");
        seedFact(worldA, speaker, sourceId, "The watchtower bell is broken", List.of(player));

        NpcKnowledgeTransferResult first = NpcKnowledgeTransferLifecycle.transfer(
                worldA, speaker, listener, sourceId, 500L, 64, 64
        );
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, first.status());
        copyStores(worldA, worldB);

        MemoryEvent reloadedEvidence = MemoryEventStore.forWorld(worldB)
                .findById(listener, first.evidenceEventId())
                .orElseThrow();
        SemanticMemoryEntry reloadedBelief = SemanticMemoryStore.forWorld(worldB)
                .findMatching(listener, entry -> entry.sourceEventIds().contains(first.evidenceEventId()))
                .orElseThrow();
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, reloadedEvidence.provenance());
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, reloadedBelief.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, reloadedBelief.provenance());
        assertEquals(List.of(first.evidenceEventId()), reloadedBelief.sourceEventIds());

        NpcKnowledgeTransferResult replay = NpcKnowledgeTransferLifecycle.transfer(
                worldB, speaker, listener, sourceId, 500L, 64, 64
        );
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, replay.status());
        assertEquals(first.evidenceEventId(), replay.evidenceEventId());
        assertEquals(reloadedBelief.id(), replay.semanticEntryId());
        assertEquals(1, MemoryEventStore.forWorld(worldB).getRecent(listener, 64).size());
        assertEquals(1, SemanticMemoryStore.forWorld(worldB).getRecent(listener, 64).size());
    }

    @Test
    void transferPreservesGlobalPrivateAndSharedSemanticScopeAndPlayerPrivacy() {
        UUID playerA = UUID.fromString("00000000-0000-0000-0000-000000071001");
        UUID playerB = UUID.fromString("00000000-0000-0000-0000-000000071002");
        UUID entityX = UUID.fromString("00000000-0000-0000-0000-000000071003");

        ScopeFixture global = transferFixture(
                tempDir.resolve("scope-global"),
                71_100L,
                "The public well is repaired",
                List.of()
        );
        SemanticMemoryEntry globalBelief = singleListenerBelief(global);
        assertEquals(List.of(), globalBelief.relatedEntities());
        assertTrue(contextContains(global.world(), global.listener(), playerA, globalBelief.statement()));
        assertTrue(contextContains(global.world(), global.listener(), playerB, globalBelief.statement()));

        ScopeFixture privateScope = transferFixture(
                tempDir.resolve("scope-private"),
                71_200L,
                "Player A stores grain in the east barn",
                List.of(playerA)
        );
        SemanticMemoryEntry privateBelief = singleListenerBelief(privateScope);
        assertEquals(List.of(playerA), privateBelief.relatedEntities());
        assertFalse(privateBelief.relatedEntities().contains(privateScope.speaker()));
        assertTrue(contextContains(privateScope.world(), privateScope.listener(), playerA, privateBelief.statement()));
        assertFalse(contextContains(privateScope.world(), privateScope.listener(), playerB, privateBelief.statement()));
        assertEquals(List.of(), Memory2DialogueHistory.load(
                privateScope.world(), privateScope.listener(), playerA));

        Memory2DialogueIngestor.record(
                privateScope.world(),
                privateScope.listener(),
                playerA,
                800L,
                "Do you remember the barn?",
                "I remember our conversation.",
                64,
                8_000L
        );
        assertEquals(List.of(
                new WorkingMemoryMessage("user", "Do you remember the barn?"),
                new WorkingMemoryMessage("assistant", "I remember our conversation.")
        ), Memory2DialogueHistory.load(privateScope.world(), privateScope.listener(), playerA));

        ScopeFixture shared = transferFixture(
                tempDir.resolve("scope-shared"),
                71_300L,
                "Player A and the guild share the cellar",
                List.of(entityX, playerA)
        );
        SemanticMemoryEntry sharedBelief = singleListenerBelief(shared);
        assertEquals(sorted(entityX, playerA), sorted(sharedBelief.relatedEntities()));
        assertFalse(sharedBelief.relatedEntities().contains(shared.speaker()));
        assertTrue(contextContains(shared.world(), shared.listener(), playerA, sharedBelief.statement()));
        assertFalse(contextContains(shared.world(), shared.listener(), playerB, sharedBelief.statement()));
    }

    @Test
    void independentNpcPairsRemainExactlyIsolated() {
        Path world = tempDir.resolve("pairs");
        UUID speakerA = UUID.fromString("00000000-0000-0000-0000-000000072001");
        UUID listenerB = UUID.fromString("00000000-0000-0000-0000-000000072002");
        UUID speakerD = UUID.fromString("00000000-0000-0000-0000-000000072003");
        UUID listenerC = UUID.fromString("00000000-0000-0000-0000-000000072004");
        UUID sourceA = UUID.fromString("00000000-0000-0000-0000-000000072005");
        UUID sourceD = UUID.fromString("00000000-0000-0000-0000-000000072006");
        seedFact(world, speakerA, sourceA, "A says the west gate is open", List.of());
        seedFact(world, speakerD, sourceD, "D says the south bridge is closed", List.of());

        NpcKnowledgeTransferResult ab = NpcKnowledgeTransferLifecycle.transfer(
                world, speakerA, listenerB, sourceA, 900L, 64, 64
        );
        List<UUID> bBefore = SemanticMemoryStore.forWorld(world).getRecent(listenerB, 64)
                .stream().map(SemanticMemoryEntry::id).toList();
        NpcKnowledgeTransferResult dc = NpcKnowledgeTransferLifecycle.transfer(
                world, speakerD, listenerC, sourceD, 901L, 64, 64
        );

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, ab.status());
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, dc.status());
        assertEquals(bBefore, SemanticMemoryStore.forWorld(world).getRecent(listenerB, 64)
                .stream().map(SemanticMemoryEntry::id).toList());
        assertEquals(List.of(ab.evidenceEventId()), MemoryEventStore.forWorld(world).getRecent(listenerB, 64)
                .stream().map(MemoryEvent::id).toList());
        assertEquals(List.of(dc.evidenceEventId()), MemoryEventStore.forWorld(world).getRecent(listenerC, 64)
                .stream().map(MemoryEvent::id).toList());
        assertEquals(sourceA, SemanticMemoryStore.forWorld(world).findById(speakerA, sourceA).orElseThrow().id());
        assertEquals(sourceD, SemanticMemoryStore.forWorld(world).findById(speakerD, sourceD).orElseThrow().id());
        assertTrue(SemanticMemoryStore.forWorld(world).findById(listenerC, ab.semanticEntryId()).isEmpty());
        assertTrue(SemanticMemoryStore.forWorld(world).findById(listenerB, dc.semanticEntryId()).isEmpty());
    }

    private ScopeFixture transferFixture(
            Path world,
            long idBase,
            String statement,
            List<UUID> relatedEntities
    ) {
        UUID speaker = new UUID(0L, idBase + 1L);
        UUID listener = new UUID(0L, idBase + 2L);
        UUID sourceId = new UUID(0L, idBase + 3L);
        seedFact(world, speaker, sourceId, statement, relatedEntities);
        NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                world, speaker, listener, sourceId, 700L, 64, 64
        );
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status());
        return new ScopeFixture(world, speaker, listener, result);
    }

    private static SemanticMemoryEntry singleListenerBelief(ScopeFixture fixture) {
        List<SemanticMemoryEntry> values = SemanticMemoryStore.forWorld(fixture.world())
                .getRecent(fixture.listener(), 64);
        assertEquals(1, values.size());
        return values.getFirst();
    }

    private static boolean contextContains(Path world, UUID listener, UUID player, String statement) {
        return SemanticMemoryContextProvider.load(world, listener, player, 1_000L)
                .stream().anyMatch(line -> line.contains(statement));
    }

    private static void seedFact(
            Path world,
            UUID speaker,
            UUID sourceId,
            String statement,
            List<UUID> relatedEntities
    ) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                relatedEntities,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L,
                0L,
                80,
                100,
                List.of(UUID.nameUUIDFromBytes((sourceId + "-source").getBytes(StandardCharsets.UTF_8)))
        ), 64);
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

    private static List<UUID> sorted(UUID... values) {
        return sorted(List.of(values));
    }

    private static List<UUID> sorted(List<UUID> values) {
        List<UUID> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.comparing(UUID::toString));
        return List.copyOf(sorted);
    }

    private record ScopeFixture(
            Path world,
            UUID speaker,
            UUID listener,
            NpcKnowledgeTransferResult result
    ) {
    }
}
