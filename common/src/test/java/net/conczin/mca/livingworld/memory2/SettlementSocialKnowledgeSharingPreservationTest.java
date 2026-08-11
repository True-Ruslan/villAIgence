package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementSocialKnowledgeSharingPreservationTest {
    @TempDir
    Path tempDir;

    @Test
    void missingSocialGraphBehavesNeutralWithoutCreatingPersistence() {
        Path world = tempDir.resolve("missing-graph");
        UUID speaker = id(1);
        UUID listener = id(2);
        appendSourceFact(world, speaker, id(100), "The north gate is open");
        Path graphFile = graphFile(world);

        assertFalse(Files.exists(graphFile));

        SettlementKnowledgeFlowLifecycle.CycleResult result = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 21, 2_400L, List.of(speaker, listener), 64, 64);

        assertEquals(1, result.opportunities());
        assertEquals(0, result.sociallySuppressedTransfers());
        assertEquals(1, result.attemptedTransfers());
        assertEquals(1, result.successfulTransfers());
        assertFalse(Files.exists(graphFile));
    }

    @Test
    void adverseSuppressionLeavesGraphAndKnowledgeStoresByteStable() throws IOException {
        Path world = tempDir.resolve("byte-stable");
        UUID speaker = id(10);
        UUID listener = id(11);
        appendSourceFact(world, speaker, id(110), "The west road is flooded");
        NpcSocialGraphStore.forWorld(world).applyDelta(
                speaker, listener, new NpcSocialDelta(-75, 0, 0, 0), 100);

        Path graph = graphFile(world);
        Path semantic = world.resolve("livingworld").resolve("semantic-memory.json");
        Path memory2 = world.resolve("livingworld").resolve("memory2.json");
        byte[] graphBefore = Files.readAllBytes(graph);
        byte[] semanticBefore = Files.readAllBytes(semantic);
        byte[] memoryBefore = Files.exists(memory2) ? Files.readAllBytes(memory2) : null;

        SettlementKnowledgeFlowLifecycle.CycleResult result = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 22, 3_600L, List.of(speaker, listener), 64, 64);

        assertEquals(1, result.opportunities());
        assertEquals(1, result.sociallySuppressedTransfers());
        assertEquals(0, result.attemptedTransfers());
        assertEquals(0, result.successfulTransfers());
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(listener, 64).isEmpty());
        assertArrayEquals(graphBefore, Files.readAllBytes(graph));
        assertArrayEquals(semanticBefore, Files.readAllBytes(semantic));
        assertOptionalFileUnchanged(memory2, memoryBefore);
    }

    @Test
    void freshRootReloadRepeatsExactPairSuppression() throws IOException {
        Path sourceWorld = tempDir.resolve("source-world");
        Path reloadedWorld = tempDir.resolve("reloaded-world");
        UUID speaker = id(20);
        UUID listener = id(21);
        appendSourceFact(sourceWorld, speaker, id(120), "The granary is locked");
        NpcSocialGraphStore.forWorld(sourceWorld).applyDelta(
                speaker, listener, new NpcSocialDelta(0, 0, 75, 0), 100);
        copyPersistence(sourceWorld, reloadedWorld, "semantic-memory.json", "npc-social-graph.json");

        SettlementKnowledgeFlowLifecycle.CycleResult result = SettlementKnowledgeFlowLifecycle.runCycle(
                reloadedWorld, 23, 4_800L, List.of(speaker, listener), 64, 64);

        assertEquals(1, result.opportunities());
        assertEquals(1, result.sociallySuppressedTransfers());
        assertEquals(0, result.attemptedTransfers());
        assertEquals(0, result.successfulTransfers());
        assertTrue(SemanticMemoryStore.forWorld(reloadedWorld).getRecent(listener, 64).isEmpty());
    }

    @Test
    void positiveSocialStateCannotChangeSelectorTargetFanoutOrOpportunityBounds() {
        Path world = tempDir.resolve("positive-state");
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        UUID speaker = id(30);
        UUID listenerA = id(31);
        UUID listenerB = id(32);
        UUID listenerC = id(33);
        List<UUID> residents = List.of(speaker, listenerA, listenerB, listenerC);
        appendSourceFact(world, speaker, id(130), "The quarry reopened");

        SettlementKnowledgeFlowSelector.SelectionResult before = SettlementKnowledgeFlowSelector.select(
                store, 24, 6_000L, residents);
        SettlementKnowledgeFlowSelector.Opportunity selected = before.opportunities().getFirst();
        NpcSocialGraphStore.forWorld(world).applyDelta(
                selected.speakerNpcId(),
                selected.listenerNpcId(),
                new NpcSocialDelta(60, 70, 0, 60),
                100
        );
        SettlementKnowledgeFlowSelector.SelectionResult after = SettlementKnowledgeFlowSelector.select(
                store, 24, 6_000L, residents);

        assertEquals(before, after);
        assertTrue(after.opportunities().size() <= SettlementKnowledgeFlowSelector.MAX_OPPORTUNITIES_PER_CYCLE);
        assertTrue(after.opportunities().size() <= SettlementKnowledgeFlowSelector.MAX_FANOUT_PER_SOURCE_PER_CYCLE);

        SettlementKnowledgeFlowLifecycle.CycleResult cycle = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 24, 6_000L, residents, 64, 64);
        assertEquals(0, cycle.sociallySuppressedTransfers());
        assertEquals(cycle.opportunities(), cycle.attemptedTransfers());
    }

    @Test
    void selectorSourceDoesNotDependOnSocialGraph() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowSelector.java"));

        assertFalse(source.contains("NpcSocialGraphStore"));
        assertFalse(source.contains("PersonalitySocialInfluencePolicy"));
        assertFalse(source.contains("SettlementSocialKnowledgeSharingPolicy"));
    }

    private static void appendSourceFact(Path world, UUID speaker, UUID sourceId, String statement) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                sourceId,
                speaker,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                List.of(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                100L,
                0L,
                90,
                100,
                List.of(id(900))
        ), 64);
    }

    private static Path graphFile(Path world) {
        return world.resolve("livingworld").resolve("npc-social-graph.json");
    }

    private static void assertOptionalFileUnchanged(Path file, byte[] before) throws IOException {
        if (before == null) {
            assertFalse(Files.exists(file));
        } else {
            assertArrayEquals(before, Files.readAllBytes(file));
        }
    }

    private static void copyPersistence(Path sourceWorld, Path targetWorld, String... names) throws IOException {
        Path source = sourceWorld.resolve("livingworld");
        Path target = targetWorld.resolve("livingworld");
        Files.createDirectories(target);
        for (String name : names) {
            Files.copy(source.resolve(name), target.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
