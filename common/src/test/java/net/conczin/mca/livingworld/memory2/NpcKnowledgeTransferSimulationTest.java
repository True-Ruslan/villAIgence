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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeTransferSimulationTest {
    private static final UUID SPEAKER_A = UUID.fromString("00000000-0000-0000-0000-000000080001");
    private static final UUID LISTENER_B = UUID.fromString("00000000-0000-0000-0000-000000080002");
    private static final UUID SPEAKER_D = UUID.fromString("00000000-0000-0000-0000-000000080003");
    private static final UUID LISTENER_C = UUID.fromString("00000000-0000-0000-0000-000000080004");
    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-000000080005");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-000000080006");
    private static final UUID SOURCE_A = UUID.fromString("00000000-0000-0000-0000-000000080007");
    private static final UUID SOURCE_D = UUID.fromString("00000000-0000-0000-0000-000000080008");
    private static final int PRESSURE_PER_DOMAIN_PER_LISTENER = 55;
    private static final long NOW = 220_000L;

    @TempDir
    Path tempDir;

    @Test
    void longHorizonTransferredBeliefAndMultiNpcPressureRemainDeterministicAcrossOrderAndReload() throws Exception {
        Path forwardWorld = tempDir.resolve("simulation-forward");
        Path reverseWorld = tempDir.resolve("simulation-reverse");
        ScenarioResult forward = runScenario(forwardWorld, false);
        ScenarioResult reverse = runScenario(reverseWorld, true);

        assertEquals(forward, reverse);
        assertTrue(forward.listenerBContext().size() <= SemanticMemoryContextProvider.MAX_RESULTS);
        assertTrue(forward.listenerCContext().size() <= SemanticMemoryContextProvider.MAX_RESULTS);
        assertTrue(forward.listenerBContext().stream().anyMatch(line ->
                line.startsWith("BELIEF | provenance=NPC_TOLD")
                        && line.contains("The northern road is blocked")));
        assertFalse(forward.foreignPlayerContext().stream().anyMatch(line ->
                line.contains("The northern road is blocked")));
        assertTrue(forward.listenerCContext().stream().anyMatch(line ->
                line.startsWith("BELIEF | provenance=NPC_TOLD")
                        && line.contains("The public mill is closed")));

        String promptSection = SemanticMemoryContextFormatter.promptSection(forward.listenerBContext());
        assertTrue(promptSection.contains("Current observed factual context wins on conflict."));
        assertTrue(promptSection.contains("Confidence never converts a BELIEF into a FACT."));

        Path reloadedWorld = tempDir.resolve("simulation-reloaded");
        copyStores(forwardWorld, reloadedWorld);
        assertEquals(forward.snapshot(), snapshot(reloadedWorld));
        assertEquals(forward.listenerBContext(),
                SemanticMemoryContextProvider.load(reloadedWorld, LISTENER_B, PLAYER_A, NOW));
        assertEquals(forward.listenerCContext(),
                SemanticMemoryContextProvider.load(reloadedWorld, LISTENER_C, PLAYER_B, NOW));
    }

    @Test
    void transferredBeliefAndEvidenceRemainEvictableUnderExistingPolicies() {
        Path world = tempDir.resolve("evictable");
        seedSources(world);
        NpcKnowledgeTransferResult transfer = NpcKnowledgeTransferLifecycle.transfer(
                world, SPEAKER_A, LISTENER_B, SOURCE_A, 100L, 64, 64
        );
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, transfer.status());

        SemanticMemoryEntry strongFact = new SemanticMemoryEntry(
                UUID.fromString("00000000-0000-0000-0000-000000089001"),
                LISTENER_B,
                SemanticMemoryEntry.Kind.FACT,
                "Current server-observed gate state",
                List.of(PLAYER_A),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                300L,
                0L,
                100,
                100,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000089002"))
        );
        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(world);
        semanticStore.append(strongFact, 1);
        assertEquals(List.of(strongFact.id()),
                semanticStore.getRecent(LISTENER_B, 16).stream().map(SemanticMemoryEntry::id).toList());
        assertTrue(semanticStore.findMatching(
                LISTENER_B,
                entry -> entry.provenance() == MemoryEvent.Provenance.NPC_TOLD
        ).isEmpty());

        MemoryEvent strongEvent = new MemoryEvent(
                UUID.fromString("00000000-0000-0000-0000-000000089003"),
                LISTENER_B,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                "Current strong observed relationship transition",
                List.of(LISTENER_B),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                300L,
                0L,
                100,
                100,
                100,
                List.of()
        );
        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        eventStore.append(strongEvent, 1);
        assertEquals(List.of(strongEvent.id()),
                eventStore.getRecent(LISTENER_B, 16).stream().map(MemoryEvent::id).toList());
        assertTrue(eventStore.findById(LISTENER_B, transfer.evidenceEventId()).isEmpty());
    }

    private ScenarioResult runScenario(Path world, boolean reversePressureOrder) {
        seedSources(world);

        NpcKnowledgeTransferResult firstA = NpcKnowledgeTransferLifecycle.transfer(
                world, SPEAKER_A, LISTENER_B, SOURCE_A, 100L, 128, 128
        );
        NpcKnowledgeTransferResult replayA = NpcKnowledgeTransferLifecycle.transfer(
                world, SPEAKER_A, LISTENER_B, SOURCE_A, 100L, 128, 128
        );
        NpcKnowledgeTransferResult laterA = NpcKnowledgeTransferLifecycle.transfer(
                world, SPEAKER_A, LISTENER_B, SOURCE_A, 36_100L, 128, 128
        );
        NpcKnowledgeTransferResult firstD = NpcKnowledgeTransferLifecycle.transfer(
                world, SPEAKER_D, LISTENER_C, SOURCE_D, 200L, 128, 128
        );

        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, firstA.status());
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, replayA.status());
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, laterA.status());
        assertEquals(NpcKnowledgeTransferResult.Status.ADMITTED, firstD.status());
        assertEquals(firstA.evidenceEventId(), replayA.evidenceEventId());
        assertNotEquals(firstA.evidenceEventId(), laterA.evidenceEventId());

        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(world);
        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        for (int step = 0; step < PRESSURE_PER_DOMAIN_PER_LISTENER; step++) {
            int index = reversePressureOrder
                    ? PRESSURE_PER_DOMAIN_PER_LISTENER - 1 - step
                    : step;
            semanticStore.append(weakSemantic(LISTENER_B, PLAYER_A, index, false), 128);
            semanticStore.append(weakSemantic(LISTENER_C, null, index, true), 128);
            eventStore.append(weakPlayerDialogue(LISTENER_B, PLAYER_A, index), 128);
            eventStore.append(weakGlobalObservation(LISTENER_C, index), 128);
        }

        List<String> bContext = SemanticMemoryContextProvider.load(world, LISTENER_B, PLAYER_A, NOW);
        List<String> foreignContext = SemanticMemoryContextProvider.load(world, LISTENER_B, PLAYER_B, NOW);
        List<String> cContext = SemanticMemoryContextProvider.load(world, LISTENER_C, PLAYER_B, NOW);
        return new ScenarioResult(snapshot(world), bContext, foreignContext, cContext);
    }

    private static ScenarioSnapshot snapshot(Path world) {
        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(world);
        MemoryEventStore eventStore = MemoryEventStore.forWorld(world);
        return new ScenarioSnapshot(
                semanticStore.getRecent(LISTENER_B, 128).stream().map(SemanticMemoryEntry::id).toList(),
                semanticStore.getRecent(LISTENER_C, 128).stream().map(SemanticMemoryEntry::id).toList(),
                eventStore.getRecent(LISTENER_B, 128).stream().map(MemoryEvent::id).toList(),
                eventStore.getRecent(LISTENER_C, 128).stream().map(MemoryEvent::id).toList()
        );
    }

    private static void seedSources(Path world) {
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        store.append(new SemanticMemoryEntry(
                SOURCE_A,
                SPEAKER_A,
                SemanticMemoryEntry.Kind.FACT,
                "The northern road is blocked",
                List.of(PLAYER_A),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                90,
                100,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000080009"))
        ), 128);
        store.append(new SemanticMemoryEntry(
                SOURCE_D,
                SPEAKER_D,
                SemanticMemoryEntry.Kind.BELIEF,
                "The public mill is closed",
                List.of(),
                MemoryEvent.Provenance.PLAYER_TOLD,
                20L,
                0L,
                70,
                70,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000080010"))
        ), 128);
    }

    private static SemanticMemoryEntry weakSemantic(
            UUID listener,
            UUID player,
            int index,
            boolean global
    ) {
        long idBase = global ? 810_000L : 800_000L;
        return new SemanticMemoryEntry(
                new UUID(0L, idBase + index),
                listener,
                SemanticMemoryEntry.Kind.BELIEF,
                (global ? "global weak semantic " : "private weak semantic ") + index,
                global ? List.of() : List.of(player),
                MemoryEvent.Provenance.INFERRED,
                200_000L + index,
                0L,
                0,
                0,
                List.of(new UUID(0L, idBase + 10_000L + index))
        );
    }

    private static MemoryEvent weakPlayerDialogue(UUID listener, UUID player, int index) {
        return new MemoryEvent(
                new UUID(0L, 900_000L + index),
                listener,
                MemoryEvent.Type.DIALOGUE,
                "weak player dialogue " + index,
                List.of(listener, player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                210_000L + index,
                0L,
                0,
                0,
                0,
                List.of()
        );
    }

    private static MemoryEvent weakGlobalObservation(UUID listener, int index) {
        return new MemoryEvent(
                new UUID(0L, 910_000L + index),
                listener,
                MemoryEvent.Type.OBSERVATION,
                "weak global observation " + index,
                List.of(listener),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                210_000L + index,
                0L,
                0,
                0,
                0,
                List.of()
        );
    }

    private static void copyStores(Path sourceWorld, Path targetWorld) throws Exception {
        Path targetLiving = targetWorld.resolve("livingworld");
        Files.createDirectories(targetLiving);
        Files.copy(sourceWorld.resolve("livingworld/memory2.json"),
                targetLiving.resolve("memory2.json"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(sourceWorld.resolve("livingworld/semantic-memory.json"),
                targetLiving.resolve("semantic-memory.json"), StandardCopyOption.REPLACE_EXISTING);
    }

    private record ScenarioResult(
            ScenarioSnapshot snapshot,
            List<String> listenerBContext,
            List<String> foreignPlayerContext,
            List<String> listenerCContext
    ) {
    }

    private record ScenarioSnapshot(
            List<UUID> listenerBSemanticIds,
            List<UUID> listenerCSemanticIds,
            List<UUID> listenerBEventIds,
            List<UUID> listenerCEventIds
    ) {
    }
}
