package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongHorizonMemorySimulationTest {
    @TempDir
    Path tempDir;

    @Test
    void multiDayPressureAndRepeatedReloadPreserveExactSurvivorsAndPromptContext() throws Exception {
        UUID npc = id(100);
        UUID player = id(101);
        UUID semanticOldId = id(102);
        UUID episodicOldId = id(103);
        long nowGameTime = 81L * SemanticMemoryRetentionPolicy.DECAY_STEP_TICKS;

        Path sessionA = tempDir.resolve("session-a");
        populateMultiDayScenario(sessionA, npc, player, semanticOldId, episodicOldId);

        List<UUID> semanticA = semanticIds(sessionA, npc);
        List<UUID> episodicA = episodicIds(sessionA, npc);
        List<String> semanticContextA = SemanticMemoryContextProvider.load(sessionA, npc, player, nowGameTime);
        List<String> episodicContextA = Memory2ContextProvider.load(sessionA, npc, player, nowGameTime);

        assertTrue(semanticA.contains(semanticOldId));
        assertTrue(episodicA.contains(episodicOldId));
        assertTrue(semanticContextA.stream().anyMatch(line -> line.contains("old-durable-semantic")));
        assertTrue(episodicContextA.stream().anyMatch(line -> line.contains("old-important-relationship")));

        Path sessionB = tempDir.resolve("session-b");
        copyMemoryFiles(sessionA, sessionB);
        assertEquals(semanticA, semanticIds(sessionB, npc));
        assertEquals(episodicA, episodicIds(sessionB, npc));
        assertEquals(semanticContextA, SemanticMemoryContextProvider.load(sessionB, npc, player, nowGameTime));
        assertEquals(episodicContextA, Memory2ContextProvider.load(sessionB, npc, player, nowGameTime));

        Path sessionC = tempDir.resolve("session-c");
        copyMemoryFiles(sessionB, sessionC);
        assertEquals(semanticA, semanticIds(sessionC, npc));
        assertEquals(episodicA, episodicIds(sessionC, npc));
        assertEquals(semanticContextA, SemanticMemoryContextProvider.load(sessionC, npc, player, nowGameTime));
        assertEquals(episodicContextA, Memory2ContextProvider.load(sessionC, npc, player, nowGameTime));
    }

    @Test
    void foreignHighDurabilityMemoryConsumesNoRecentOrDurableSlots() {
        UUID npc = id(200);
        UUID currentPlayer = id(201);
        UUID foreignPlayer = id(202);
        Path world = tempDir.resolve("privacy-pressure");

        SemanticMemoryStore semantic = SemanticMemoryStore.forWorld(world);
        semantic.append(semanticBelief(
                id(210), npc, currentPlayer, 1L,
                "current-player-old-durable-semantic", 100, 100
        ), 128);
        for (int i = 0; i < 24; i++) {
            semantic.append(semanticBelief(
                    id(300 + i), npc, currentPlayer, 1_000_000L + i,
                    "current-player-recent-semantic-" + i, 0, 0
            ), 128);
        }
        for (int i = 0; i < 20; i++) {
            semantic.append(semanticBelief(
                    id(400 + i), npc, foreignPlayer, 10_000L + i,
                    "foreign-durable-semantic-" + i, 100, 100
            ), 128);
        }

        MemoryEventStore episodic = MemoryEventStore.forWorld(world);
        episodic.append(relationshipChange(
                id(220), npc, currentPlayer, 1L,
                "current-player-old-important-relationship", 100, 100
        ), 128);
        for (int i = 0; i < 24; i++) {
            episodic.append(dialogue(
                    id(500 + i), npc, currentPlayer, 1_000_000L + i,
                    "current-player-recent-dialogue-" + i, 0, 0
            ), 128);
        }
        for (int i = 0; i < 20; i++) {
            episodic.append(relationshipChange(
                    id(600 + i), npc, foreignPlayer, 10_000L + i,
                    "foreign-durable-relationship-" + i, 100, 100
            ), 128);
        }

        List<String> semanticContext = SemanticMemoryContextProvider.load(
                world, npc, currentPlayer, 1_000_100L
        );
        List<String> episodicContext = Memory2ContextProvider.load(
                world, npc, currentPlayer, 1_000_100L
        );

        assertTrue(semanticContext.stream().anyMatch(
                line -> line.contains("current-player-old-durable-semantic")
        ));
        assertTrue(semanticContext.stream().noneMatch(line -> line.contains("foreign-durable-semantic-")));
        assertTrue(episodicContext.stream().anyMatch(
                line -> line.contains("current-player-old-important-relationship")
        ));
        assertTrue(episodicContext.stream().noneMatch(line -> line.contains("foreign-durable-relationship-")));
    }

    @Test
    void hundredsOfEventsRemainDeterministicAcrossInputOrderAndGameTimePressure() {
        UUID npc = id(700);
        UUID player = id(701);
        long nowGameTime = 300L * MemoryEventRetentionPolicy.DECAY_STEP_TICKS;

        List<SemanticMemoryEntry> semanticForward = new ArrayList<>();
        List<MemoryEvent> episodicForward = new ArrayList<>();
        for (int i = 0; i < 240; i++) {
            long gameTime = (i + 1L) * MemoryEventRetentionPolicy.DECAY_STEP_TICKS;
            int importance = i % 37 == 0 ? 100 : i % 23;
            int confidence = i % 41 == 0 ? 100 : i % 29;
            semanticForward.add(semanticBelief(
                    id(1_000 + i), npc, player, gameTime,
                    "simulation-semantic-" + i, importance, confidence
            ));
            episodicForward.add(i % 11 == 0
                    ? relationshipChange(
                            id(2_000 + i), npc, player, gameTime,
                            "simulation-relationship-" + i, importance, confidence
                    )
                    : dialogue(
                            id(2_000 + i), npc, player, gameTime,
                            "simulation-dialogue-" + i, importance, confidence
                    ));
        }

        List<SemanticMemoryEntry> semanticReverse = new ArrayList<>(semanticForward);
        List<MemoryEvent> episodicReverse = new ArrayList<>(episodicForward);
        Collections.reverse(semanticReverse);
        Collections.reverse(episodicReverse);

        List<SemanticMemoryEntry> retainedSemanticForward = SemanticMemoryRetentionPolicy.selectRetained(
                semanticForward, 64, nowGameTime
        );
        List<SemanticMemoryEntry> retainedSemanticReverse = SemanticMemoryRetentionPolicy.selectRetained(
                semanticReverse, 64, nowGameTime
        );
        assertEquals(idsOfSemantic(retainedSemanticForward), idsOfSemantic(retainedSemanticReverse));

        List<MemoryEvent> retainedEpisodicForward = MemoryEventRetentionPolicy.selectRetained(
                episodicForward, 64, nowGameTime
        );
        List<MemoryEvent> retainedEpisodicReverse = MemoryEventRetentionPolicy.selectRetained(
                episodicReverse, 64, nowGameTime
        );
        assertEquals(idsOfEvents(retainedEpisodicForward), idsOfEvents(retainedEpisodicReverse));

        List<SemanticMemoryEntry> selectedSemanticForward = LongHorizonCandidateSelector.select(
                retainedSemanticForward,
                32,
                semanticNewestFirst(),
                semanticDurableFirst(nowGameTime),
                SemanticMemoryEntry::id
        );
        List<SemanticMemoryEntry> selectedSemanticReverse = LongHorizonCandidateSelector.select(
                retainedSemanticReverse,
                32,
                semanticNewestFirst(),
                semanticDurableFirst(nowGameTime),
                SemanticMemoryEntry::id
        );
        assertEquals(idsOfSemantic(selectedSemanticForward), idsOfSemantic(selectedSemanticReverse));
        assertEquals(32, selectedSemanticForward.size());

        List<MemoryEvent> selectedEpisodicForward = LongHorizonCandidateSelector.select(
                retainedEpisodicForward,
                32,
                episodicNewestFirst(),
                episodicDurableFirst(nowGameTime),
                MemoryEvent::id
        );
        List<MemoryEvent> selectedEpisodicReverse = LongHorizonCandidateSelector.select(
                retainedEpisodicReverse,
                32,
                episodicNewestFirst(),
                episodicDurableFirst(nowGameTime),
                MemoryEvent::id
        );
        assertEquals(idsOfEvents(selectedEpisodicForward), idsOfEvents(selectedEpisodicReverse));
        assertEquals(32, selectedEpisodicForward.size());
    }

    private static void populateMultiDayScenario(
            Path world,
            UUID npc,
            UUID player,
            UUID semanticOldId,
            UUID episodicOldId
    ) {
        SemanticMemoryStore semantic = SemanticMemoryStore.forWorld(world);
        semantic.append(semanticBelief(
                semanticOldId, npc, player, 100L,
                "old-durable-semantic", 100, 100
        ), 64);

        MemoryEventStore episodic = MemoryEventStore.forWorld(world);
        episodic.append(relationshipChange(
                episodicOldId, npc, player, 100L,
                "old-important-relationship", 100, 100
        ), 64);

        for (int i = 0; i < 80; i++) {
            long gameTime = (i + 1L) * MemoryEventRetentionPolicy.DECAY_STEP_TICKS;
            semantic.append(semanticBelief(
                    id(3_000 + i), npc, player, gameTime,
                    "multi-day-weak-semantic-" + i, 0, 0
            ), 64);
            episodic.append(dialogue(
                    id(4_000 + i), npc, player, gameTime,
                    "multi-day-weak-dialogue-" + i, 0, 0
            ), 64);
        }
    }

    private static void copyMemoryFiles(Path sourceWorld, Path targetWorld) throws Exception {
        Path targetLivingWorld = targetWorld.resolve("livingworld");
        Files.createDirectories(targetLivingWorld);
        Files.copy(
                sourceWorld.resolve("livingworld").resolve("semantic-memory.json"),
                targetLivingWorld.resolve("semantic-memory.json")
        );
        Files.copy(
                sourceWorld.resolve("livingworld").resolve("memory2.json"),
                targetLivingWorld.resolve("memory2.json")
        );
    }

    private static List<UUID> semanticIds(Path world, UUID npc) {
        return SemanticMemoryStore.forWorld(world).getRecent(npc, 64).stream()
                .map(SemanticMemoryEntry::id)
                .toList();
    }

    private static List<UUID> episodicIds(Path world, UUID npc) {
        return MemoryEventStore.forWorld(world).getRecent(npc, 64).stream()
                .map(MemoryEvent::id)
                .toList();
    }

    private static SemanticMemoryEntry semanticBelief(
            UUID id,
            UUID npc,
            UUID player,
            long gameTime,
            String statement,
            int importance,
            int confidence
    ) {
        return new SemanticMemoryEntry(
                id,
                npc,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                gameTime,
                1_700_000_000_000L + gameTime,
                importance,
                confidence,
                List.of(id(9_000 + id.getLeastSignificantBits()))
        );
    }

    private static MemoryEvent relationshipChange(
            UUID id,
            UUID npc,
            UUID player,
            long gameTime,
            String summary,
            int importance,
            int confidence
    ) {
        return new MemoryEvent(
                id,
                npc,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                summary,
                List.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                1_700_000_000_000L + gameTime,
                importance,
                50,
                confidence,
                List.of(),
                null,
                new MemoryEvent.RelationshipTransition(0, 0, 0, 0, 1, 0, 0, 0)
        );
    }

    private static MemoryEvent dialogue(
            UUID id,
            UUID npc,
            UUID player,
            long gameTime,
            String summary,
            int importance,
            int confidence
    ) {
        return new MemoryEvent(
                id,
                npc,
                MemoryEvent.Type.DIALOGUE,
                summary,
                List.of(npc, player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                gameTime,
                1_700_000_000_000L + gameTime,
                importance,
                0,
                confidence,
                List.of(),
                new MemoryEvent.DialogueExchange("message-" + id, "reply-" + id)
        );
    }

    private static Comparator<SemanticMemoryEntry> semanticNewestFirst() {
        return Comparator
                .comparingLong(SemanticMemoryEntry::gameTime).reversed()
                .thenComparing(Comparator.comparingLong(SemanticMemoryEntry::createdAtEpochMillis).reversed())
                .thenComparing(entry -> entry.id().toString(), Comparator.reverseOrder());
    }

    private static Comparator<SemanticMemoryEntry> semanticDurableFirst(long gameTime) {
        return Comparator
                .comparingLong((SemanticMemoryEntry entry) ->
                        SemanticMemoryRetentionPolicy.effectiveRetentionScore(entry, gameTime))
                .reversed()
                .thenComparing(Comparator.comparingInt(SemanticMemoryEntry::importance).reversed())
                .thenComparing(Comparator.comparingInt(SemanticMemoryEntry::confidence).reversed())
                .thenComparing(Comparator.comparingInt(
                        (SemanticMemoryEntry entry) -> entry.sourceEventIds().size()).reversed())
                .thenComparing(Comparator.comparingLong(SemanticMemoryEntry::gameTime).reversed())
                .thenComparing(Comparator.comparingLong(SemanticMemoryEntry::createdAtEpochMillis).reversed())
                .thenComparing(entry -> entry.id().toString());
    }

    private static Comparator<MemoryEvent> episodicNewestFirst() {
        return Comparator
                .comparingLong(MemoryEvent::gameTime).reversed()
                .thenComparing(Comparator.comparingLong(MemoryEvent::createdAtEpochMillis).reversed())
                .thenComparing(event -> event.id().toString(), Comparator.reverseOrder());
    }

    private static Comparator<MemoryEvent> episodicDurableFirst(long gameTime) {
        return Comparator
                .comparingLong((MemoryEvent event) ->
                        MemoryEventRetentionPolicy.effectiveRetentionScore(event, gameTime))
                .reversed()
                .thenComparing(Comparator.comparingInt(MemoryEvent::importance).reversed())
                .thenComparing(Comparator.comparingInt(MemoryEvent::confidence).reversed())
                .thenComparing(Comparator.comparingInt(
                        (MemoryEvent event) -> Math.abs(event.emotionalWeight())).reversed())
                .thenComparing(Comparator.comparingInt(
                        (MemoryEvent event) -> MemoryEventRetentionPolicy.typeContribution(event.type())).reversed())
                .thenComparing(Comparator.comparingInt(
                        (MemoryEvent event) -> MemoryEventRetentionPolicy.provenanceContribution(event.provenance())).reversed())
                .thenComparing(Comparator.comparingLong(MemoryEvent::gameTime).reversed())
                .thenComparing(Comparator.comparingLong(MemoryEvent::createdAtEpochMillis).reversed())
                .thenComparing(event -> event.id().toString());
    }

    private static List<UUID> idsOfSemantic(List<SemanticMemoryEntry> entries) {
        return entries.stream().map(SemanticMemoryEntry::id).toList();
    }

    private static List<UUID> idsOfEvents(List<MemoryEvent> events) {
        return events.stream().map(MemoryEvent::id).toList();
    }

    private static UUID id(long value) {
        return new UUID(0L, value);
    }
}
