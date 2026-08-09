package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticContradictionSimulationTest {
    private static final int NOISE_COUNT = 240;
    private static final int PRESSURE_CAPACITY = 64;

    @TempDir
    Path tempDir;

    @Test
    void multiNpcContradictionsRemainDeterministicAcrossPressurePrivacyAndTruthClasses() {
        ScenarioSnapshot forward = runScenario(tempDir.resolve("forward"), false);
        ScenarioSnapshot reverse = runScenario(tempDir.resolve("reverse"), true);
        assertEquals(forward, reverse);
    }

    private ScenarioSnapshot runScenario(Path world, boolean reversePressure) {
        UUID playerA = id(90);
        UUID playerB = id(91);
        UUID sharedEntity = id(92);
        UUID globalNpc = id(100);
        UUID privateNpc = id(200);
        UUID sharedNpc = id(300);

        SemanticMemoryEntry globalFact = entry(
                id(1001), globalNpc, SemanticMemoryEntry.Kind.FACT,
                "The bridge is intact", List.of(), MemoryEvent.Provenance.SYSTEM_OBSERVED, 100, 100, id(9001));
        SemanticMemoryEntry globalRumor = entry(
                id(1002), globalNpc, SemanticMemoryEntry.Kind.BELIEF,
                "The bridge is destroyed", List.of(), MemoryEvent.Provenance.NPC_TOLD, 90, 80, id(9002));
        SemanticMemoryEntry privateOne = entry(
                id(2001), privateNpc, SemanticMemoryEntry.Kind.BELIEF,
                "Player A hid the map", List.of(playerA), MemoryEvent.Provenance.PLAYER_TOLD, 70, 70, id(9101));
        SemanticMemoryEntry privateTwo = entry(
                id(2002), privateNpc, SemanticMemoryEntry.Kind.BELIEF,
                "Player A burned the map", List.of(playerA), MemoryEvent.Provenance.NPC_TOLD, 70, 70, id(9102));
        SemanticMemoryEntry sharedOne = entry(
                id(3001), sharedNpc, SemanticMemoryEntry.Kind.FACT,
                "The guild cellar is open", List.of(sharedEntity, playerA),
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 90, 100, id(9201));
        SemanticMemoryEntry sharedTwo = entry(
                id(3002), sharedNpc, SemanticMemoryEntry.Kind.BELIEF,
                "The guild cellar is sealed", List.of(playerA, sharedEntity),
                MemoryEvent.Provenance.NPC_TOLD, 80, 80, id(9202));

        seed(world, globalFact, globalRumor, privateOne, privateTwo, sharedOne, sharedTwo);
        SemanticContradictionResult global = record(world, globalNpc, globalFact, globalRumor, 500L);
        SemanticContradictionResult privateRelation = record(world, privateNpc, privateOne, privateTwo, 600L);
        SemanticContradictionResult shared = record(world, sharedNpc, sharedOne, sharedTwo, 700L);

        addPressure(world, globalNpc, playerA, reversePressure);

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> globalHistory =
                SemanticContradictionHistory.load(world, globalNpc, playerA, 8);
        List<SemanticContradictionHistory.ResolvedSemanticContradiction> privateA =
                SemanticContradictionHistory.load(world, privateNpc, playerA, 8);
        List<SemanticContradictionHistory.ResolvedSemanticContradiction> privateB =
                SemanticContradictionHistory.load(world, privateNpc, playerB, 8);
        List<SemanticContradictionHistory.ResolvedSemanticContradiction> sharedA =
                SemanticContradictionHistory.load(world, sharedNpc, playerA, 8);
        List<SemanticContradictionHistory.ResolvedSemanticContradiction> sharedB =
                SemanticContradictionHistory.load(world, sharedNpc, playerB, 8);

        assertEquals(List.of(global.eventId()), globalHistory.stream().map(r -> r.evidence().id()).toList());
        assertEquals(List.of(privateRelation.eventId()), privateA.stream().map(r -> r.evidence().id()).toList());
        assertTrue(privateB.isEmpty());
        assertEquals(List.of(shared.eventId()), sharedA.stream().map(r -> r.evidence().id()).toList());
        assertTrue(sharedB.isEmpty());

        SemanticMemoryEntry retainedFact = findLogical(world, globalNpc, globalFact);
        SemanticMemoryEntry retainedRumor = findLogical(world, globalNpc, globalRumor);
        assertEquals(SemanticMemoryEntry.Kind.FACT, retainedFact.kind());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, retainedFact.provenance());
        assertEquals(100, retainedFact.confidence());
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, retainedRumor.kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, retainedRumor.provenance());
        assertEquals(80, retainedRumor.confidence());

        List<String> semanticContext = SemanticMemoryContextProvider.load(world, globalNpc, playerA, 2_000L);
        assertTrue(semanticContext.size() <= SemanticMemoryContextProvider.MAX_RESULTS);
        assertTrue(semanticContext.stream().anyMatch(line -> line.contains("The bridge is intact")
                && line.contains("FACT") && line.contains("SYSTEM_OBSERVED")));
        assertTrue(semanticContext.stream().anyMatch(line -> line.contains("The bridge is destroyed")
                && line.contains("BELIEF") && line.contains("NPC_TOLD")));
        String prompt = SemanticMemoryContextFormatter.promptSection(semanticContext);
        assertTrue(prompt.contains("Current observed factual context wins on conflict."));
        assertTrue(prompt.contains("Confidence never converts a BELIEF into a FACT."));

        List<String> episodicContext = Memory2ContextProvider.load(world, globalNpc, playerA, 2_000L);
        assertTrue(episodicContext.size() <= Memory2ContextProvider.MAX_RESULTS);
        assertFalse(episodicContext.stream().anyMatch(line -> line.contains("Semantic contradiction recorded")));
        assertTrue(MemoryEventStore.forWorld(world).findById(globalNpc, global.eventId()).isPresent());
        assertEquals(PRESSURE_CAPACITY,
                SemanticMemoryStore.forWorld(world).getRecent(globalNpc, NOISE_COUNT + 16).size());
        assertEquals(PRESSURE_CAPACITY,
                MemoryEventStore.forWorld(world).getRecent(globalNpc, NOISE_COUNT + 16).size());

        return new ScenarioSnapshot(
                globalHistory.stream().map(r -> r.evidence().id()).toList(),
                privateA.stream().map(r -> r.evidence().id()).toList(),
                sharedA.stream().map(r -> r.evidence().id()).toList(),
                SemanticMemoryStore.forWorld(world).getRecent(globalNpc, NOISE_COUNT + 16)
                        .stream().map(SemanticMemoryEntry::id).toList(),
                MemoryEventStore.forWorld(world).getRecent(globalNpc, NOISE_COUNT + 16)
                        .stream().map(MemoryEvent::id).toList(),
                semanticContext,
                episodicContext
        );
    }

    private static void addPressure(Path world, UUID npc, UUID player, boolean reverse) {
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < NOISE_COUNT; index++) order.add(index);
        if (reverse) Collections.reverse(order);

        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(world);
        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        for (int index : order) {
            semanticStore.append(new SemanticMemoryEntry(
                    id(10_000 + index),
                    npc,
                    SemanticMemoryEntry.Kind.BELIEF,
                    "Noise claim " + index,
                    List.of(),
                    MemoryEvent.Provenance.PLAYER_TOLD,
                    1_000L + index,
                    0L,
                    5,
                    5,
                    List.of(id(20_000 + index))
            ), PRESSURE_CAPACITY);
            eventStore.append(new MemoryEvent(
                    id(30_000 + index),
                    npc,
                    MemoryEvent.Type.DIALOGUE,
                    "Noise dialogue " + index,
                    List.of(npc, player),
                    MemoryEvent.Provenance.PLAYER_TOLD,
                    1_000L + index,
                    0L,
                    5,
                    0,
                    5,
                    List.of()
            ), PRESSURE_CAPACITY);
        }
    }

    private static SemanticMemoryEntry findLogical(Path world, UUID npc, SemanticMemoryEntry expected) {
        UUID logicalId = SemanticMemoryIdentity.logicalClaimId(expected);
        return SemanticMemoryStore.forWorld(world)
                .findMatching(npc, entry -> SemanticMemoryIdentity.logicalClaimId(entry).equals(logicalId))
                .orElseThrow();
    }

    private static void seed(Path world, SemanticMemoryEntry... entries) {
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        for (SemanticMemoryEntry entry : entries) store.append(entry, 256);
    }

    private static SemanticContradictionResult record(
            Path world,
            UUID npc,
            SemanticMemoryEntry first,
            SemanticMemoryEntry second,
            long gameTime
    ) {
        SemanticContradictionResult result = SemanticContradictionLifecycle.record(
                world, npc, first.id(), second.id(), gameTime, 256);
        assertEquals(SemanticContradictionResult.Status.RECORDED, result.status());
        return result;
    }

    private static SemanticMemoryEntry entry(
            UUID entryId,
            UUID owner,
            SemanticMemoryEntry.Kind kind,
            String statement,
            List<UUID> scope,
            MemoryEvent.Provenance provenance,
            int importance,
            int confidence,
            UUID sourceId
    ) {
        return new SemanticMemoryEntry(
                entryId,
                owner,
                kind,
                statement,
                scope,
                provenance,
                10L,
                0L,
                importance,
                confidence,
                List.of(sourceId)
        );
    }

    private record ScenarioSnapshot(
            List<UUID> globalContradictions,
            List<UUID> privateContradictions,
            List<UUID> sharedContradictions,
            List<UUID> retainedSemanticIds,
            List<UUID> retainedEventIds,
            List<String> semanticContext,
            List<String> episodicContext
    ) {
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
