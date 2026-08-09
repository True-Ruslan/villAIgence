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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticContradictionPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void exactEvidenceAndResolvedClaimsSurviveFreshRootWithoutDuplicatingClaimText() throws Exception {
        Path sourceWorld = tempDir.resolve("source");
        Path reloadedWorld = tempDir.resolve("reloaded");
        UUID npc = id(1);
        UUID player = id(90);
        SemanticMemoryEntry fact = new SemanticMemoryEntry(
                id(101), npc, SemanticMemoryEntry.Kind.FACT, "The north gate is open", List.of(player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 10L, 0L, 100, 100, List.of(id(901)));
        SemanticMemoryEntry belief = new SemanticMemoryEntry(
                id(102), npc, SemanticMemoryEntry.Kind.BELIEF, "The north gate is closed", List.of(player),
                MemoryEvent.Provenance.NPC_TOLD, 11L, 0L, 60, 55, List.of(id(902)));
        SemanticMemoryStore.forWorld(sourceWorld).append(fact, 64);
        SemanticMemoryStore.forWorld(sourceWorld).append(belief, 64);

        SemanticContradictionResult result = SemanticContradictionLifecycle.record(
                sourceWorld, npc, fact.id(), belief.id(), 200L, 64);
        assertEquals(SemanticContradictionResult.Status.RECORDED, result.status());
        MemoryEvent sourceEvidence = MemoryEventStore.forWorld(sourceWorld)
                .findById(npc, result.eventId()).orElseThrow();
        assertTrue(SemanticContradictionPolicy.valid(sourceEvidence));

        String memoryJson = Files.readString(sourceWorld.resolve("livingworld/memory2.json"));
        assertFalse(memoryJson.contains(fact.statement()));
        assertFalse(memoryJson.contains(belief.statement()));
        assertTrue(memoryJson.contains("semanticContradiction"));

        copyStores(sourceWorld, reloadedWorld);
        MemoryEvent reloadedEvidence = MemoryEventStore.forWorld(reloadedWorld)
                .findById(npc, result.eventId()).orElseThrow();
        assertEquals(sourceEvidence, reloadedEvidence);
        assertTrue(SemanticContradictionPolicy.valid(reloadedEvidence));

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> resolved =
                SemanticContradictionHistory.load(reloadedWorld, npc, player, 8);
        assertEquals(1, resolved.size());
        assertEquals(result.eventId(), resolved.getFirst().evidence().id());
        assertEquals(
                List.of(SemanticMemoryEntry.Kind.FACT, SemanticMemoryEntry.Kind.BELIEF).stream().sorted().toList(),
                List.of(resolved.getFirst().first().kind(), resolved.getFirst().second().kind()).stream().sorted().toList()
        );
        assertTrue(Memory2ContextProvider.load(reloadedWorld, npc, player, 1_000L)
                .stream().noneMatch(line -> line.contains("Semantic contradiction recorded")));
    }

    private static void copyStores(Path sourceWorld, Path targetWorld) throws Exception {
        Path target = targetWorld.resolve("livingworld");
        Files.createDirectories(target);
        Files.copy(sourceWorld.resolve("livingworld/memory2.json"), target.resolve("memory2.json"),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sourceWorld.resolve("livingworld/semantic-memory.json"), target.resolve("semantic-memory.json"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
