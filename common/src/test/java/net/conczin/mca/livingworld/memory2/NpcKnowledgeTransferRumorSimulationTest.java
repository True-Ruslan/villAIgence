package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeTransferRumorSimulationTest {
    private static final int NOISE_COUNT = 240;
    private static final int NOISE_CAPACITY = 64;

    @TempDir
    Path tempDir;

    @Test
    void tenNpcMultiHopRumorSimulationIsDeterministicAcrossPressureOrderAndReloads() throws Exception {
        Path forwardWorld = tempDir.resolve("forward");
        Path reverseWorld = tempDir.resolve("reverse");

        ScenarioSnapshot forward = runScenario(forwardWorld, false);
        ScenarioSnapshot reverse = runScenario(reverseWorld, true);
        assertEquals(forward, reverse);

        Path reloadOne = tempDir.resolve("reload-one");
        Path reloadTwo = tempDir.resolve("reload-two");
        copyStores(forwardWorld, reloadOne);
        copyStores(reloadOne, reloadTwo);
        assertEquals(forward, snapshot(reloadOne));
        assertEquals(forward, snapshot(reloadTwo));
    }

    private ScenarioSnapshot runScenario(Path world, boolean reverseNoise) {
        UUID playerA = id(90);
        UUID playerB = id(91);
        List<UUID> chain = new ArrayList<>();
        for (int index = 0; index < 10; index++) chain.add(id(100 + index));
        UUID originEntry = id(500);
        seedFact(world, chain.get(0), originEntry, "The bridge is intact", List.of(playerA), 10L);

        UUID currentSource = originEntry;
        NpcKnowledgeTransferResult eighth = null;
        for (int hop = 0; hop < 8; hop++) {
            NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                    world,
                    chain.get(hop),
                    chain.get(hop + 1),
                    currentSource,
                    100L + hop,
                    512,
                    512
            );
            assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, result.status(), "hop " + (hop + 1));
            currentSource = result.semanticEntryId();
            eighth = result;
        }
        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_LIMIT_REACHED,
                NpcKnowledgeTransferLifecycle.transfer(
                        world, chain.get(8), chain.get(9), currentSource, 200L, 512, 512).status());
        assertEquals(NpcKnowledgeTransferResult.Status.PROVENANCE_CYCLE,
                NpcKnowledgeTransferLifecycle.transfer(
                        world, chain.get(8), chain.get(0), currentSource, 201L, 512, 512).status());

        SemanticMemoryEntry deepBelief = SemanticMemoryStore.forWorld(world)
                .findById(chain.get(8), currentSource).orElseThrow();
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, deepBelief.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, deepBelief.provenance());
        assertEquals(List.of(playerA), deepBelief.relatedEntities());
        MemoryEvent deepEvidence = MemoryEventStore.forWorld(world)
                .findById(chain.get(8), eighth.evidenceEventId()).orElseThrow();
        assertEquals(8, deepEvidence.knowledgeTransferProvenance().hops().size());
        assertEquals(SemanticMemoryEntry.Kind.FACT, deepEvidence.knowledgeTransferProvenance().origin().originKind());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED,
                deepEvidence.knowledgeTransferProvenance().origin().originProvenance());

        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                id(501),
                chain.get(8),
                SemanticMemoryEntry.Kind.FACT,
                "The bridge is destroyed",
                List.of(playerA),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                500L,
                0L,
                100,
                100,
                List.of(id(502))
        ), 512);

        UUID corroborationListener = id(700);
        UUID originX = id(701);
        UUID originY = id(702);
        UUID sourceX = id(703);
        UUID sourceY = id(704);
        seedFact(world, originX, sourceX, "The market is closed", List.of(), 20L);
        seedFact(world, originY, sourceY, "The market is closed", List.of(), 21L);
        NpcKnowledgeTransferResult xTell = NpcKnowledgeTransferLifecycle.transfer(
                world, originX, corroborationListener, sourceX, 300L, 512, 512);
        NpcKnowledgeTransferResult yTell = NpcKnowledgeTransferLifecycle.transfer(
                world, originY, corroborationListener, sourceY, 301L, 512, 512);
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, xTell.status());
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, yTell.status());
        SemanticMemoryEntry corroborated = SemanticMemoryStore.forWorld(world)
                .getRecent(corroborationListener, 32).stream()
                .filter(entry -> entry.statement().equals("The market is closed"))
                .findFirst().orElseThrow();
        assertEquals(2, corroborated.sourceEventIds().size());
        assertTrue(corroborated.sourceEventIds().contains(xTell.evidenceEventId()));
        assertTrue(corroborated.sourceEventIds().contains(yTell.evidenceEventId()));
        assertEquals(originX, MemoryEventStore.forWorld(world)
                .findById(corroborationListener, xTell.evidenceEventId()).orElseThrow()
                .knowledgeTransferProvenance().origin().originNpcId());
        assertEquals(originY, MemoryEventStore.forWorld(world)
                .findById(corroborationListener, yTell.evidenceEventId()).orElseThrow()
                .knowledgeTransferProvenance().origin().originNpcId());

        addNoise(world, reverseNoise, playerA);

        List<String> playerAContext = SemanticMemoryContextProvider.load(
                world, chain.get(8), playerA, 2_000L);
        List<String> playerBContext = SemanticMemoryContextProvider.load(
                world, chain.get(8), playerB, 2_000L);
        assertTrue(playerAContext.size() <= SemanticMemoryContextProvider.MAX_RESULTS);
        assertTrue(playerAContext.stream().anyMatch(line -> line.contains("The bridge is intact")
                && line.contains("BELIEF") && line.contains("NPC_TOLD")));
        assertTrue(playerAContext.stream().anyMatch(line -> line.contains("The bridge is destroyed")
                && line.contains("FACT") && line.contains("SYSTEM_OBSERVED")));
        assertFalse(playerBContext.stream().anyMatch(line -> line.contains("The bridge is intact")));
        String prompt = SemanticMemoryContextFormatter.promptSection(playerAContext);
        assertTrue(prompt.contains("Current observed factual context wins on conflict."));
        assertTrue(prompt.contains("Confidence never converts a BELIEF into a FACT."));

        return snapshot(world);
    }

    private static void addNoise(Path world, boolean reverse, UUID player) {
        UUID noiseNpc = id(8_000);
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < NOISE_COUNT; index++) order.add(index);
        if (reverse) java.util.Collections.reverse(order);

        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(world);
        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        for (int index : order) {
            semanticStore.append(new SemanticMemoryEntry(
                    id(10_000 + index),
                    noiseNpc,
                    SemanticMemoryEntry.Kind.BELIEF,
                    "Noise belief " + index,
                    List.of(),
                    MemoryEvent.Provenance.PLAYER_TOLD,
                    1_000L + index,
                    0L,
                    10,
                    10,
                    List.of(id(20_000 + index))
            ), NOISE_CAPACITY);
            eventStore.append(new MemoryEvent(
                    id(30_000 + index),
                    noiseNpc,
                    MemoryEvent.Type.DIALOGUE,
                    "Noise event " + index,
                    List.of(noiseNpc, player),
                    MemoryEvent.Provenance.PLAYER_TOLD,
                    1_000L + index,
                    0L,
                    10,
                    0,
                    10,
                    List.of()
            ), NOISE_CAPACITY);
        }
        assertEquals(NOISE_CAPACITY, semanticStore.getRecent(noiseNpc, NOISE_COUNT).size());
        assertEquals(NOISE_CAPACITY, eventStore.getRecent(noiseNpc, NOISE_COUNT).size());
    }

    private static ScenarioSnapshot snapshot(Path world) {
        UUID deepNpc = id(108);
        UUID playerA = id(90);
        UUID playerB = id(91);
        UUID noiseNpc = id(8_000);
        UUID corroborationListener = id(700);
        List<SemanticMemoryEntry> deep = SemanticMemoryStore.forWorld(world).getRecent(deepNpc, 64);
        List<MemoryEvent> deepEvents = MemoryEventStore.forWorld(world).getRecent(deepNpc, 64);
        List<KnowledgeTransferProvenance> provenance = deepEvents.stream()
                .map(MemoryEvent::knowledgeTransferProvenance)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new ScenarioSnapshot(
                deep.stream().map(SemanticMemoryEntry::id).toList(),
                deepEvents.stream().map(MemoryEvent::id).toList(),
                provenance,
                SemanticMemoryStore.forWorld(world).getRecent(noiseNpc, NOISE_COUNT)
                        .stream().map(SemanticMemoryEntry::id).toList(),
                MemoryEventStore.forWorld(world).getRecent(noiseNpc, NOISE_COUNT)
                        .stream().map(MemoryEvent::id).toList(),
                SemanticMemoryStore.forWorld(world).getRecent(corroborationListener, 16)
                        .stream().filter(entry -> entry.statement().equals("The market is closed"))
                        .findFirst().orElseThrow().sourceEventIds(),
                SemanticMemoryContextProvider.load(world, deepNpc, playerA, 2_000L),
                SemanticMemoryContextProvider.load(world, deepNpc, playerB, 2_000L)
        );
    }

    private static void seedFact(
            Path world,
            UUID owner,
            UUID entryId,
            String statement,
            List<UUID> scope,
            long gameTime
    ) {
        SemanticMemoryStore.forWorld(world).append(new SemanticMemoryEntry(
                entryId,
                owner,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                scope,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                100,
                100,
                List.of(id(40_000 + Math.floorMod(entryId.hashCode(), 5_000)))
        ), 512);
    }

    private static void copyStores(Path sourceWorld, Path targetWorld) throws Exception {
        Path target = targetWorld.resolve("livingworld");
        Files.createDirectories(target);
        Files.copy(sourceWorld.resolve("livingworld/memory2.json"), target.resolve("memory2.json"),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sourceWorld.resolve("livingworld/semantic-memory.json"), target.resolve("semantic-memory.json"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private record ScenarioSnapshot(
            List<UUID> deepSemanticIds,
            List<UUID> deepEventIds,
            List<KnowledgeTransferProvenance> deepProvenance,
            List<UUID> noiseSemanticIds,
            List<UUID> noiseEventIds,
            List<UUID> corroboratingDirectSourceIds,
            List<String> playerAContext,
            List<String> playerBContext
    ) {
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
