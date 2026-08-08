package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryRetrieverTest {
    @TempDir
    Path tempDir;

    @Test
    void explicitRelevanceCanOutrankNewerHighImportanceIrrelevantMemory() {
        UUID npc = UUID.randomUUID();
        UUID relevantParticipant = UUID.randomUUID();
        UUID otherParticipant = UUID.randomUUID();
        MemoryEventStore store = new MemoryEventStore(tempDir.resolve("memory2.json"));

        MemoryEvent relevantOld = event(
                UUID.fromString("00000000-0000-0000-0000-000000000001"), npc,
                MemoryEvent.Type.RELATIONSHIP_CHANGE, Set.of(relevantParticipant),
                50L, 70, 100
        );
        MemoryEvent irrelevantNew = event(
                UUID.fromString("00000000-0000-0000-0000-000000000002"), npc,
                MemoryEvent.Type.OBSERVATION, Set.of(otherParticipant),
                100L, 100, 100
        );
        store.append(relevantOld, 10);
        store.append(irrelevantNew, 10);

        MemoryQuery query = new MemoryQuery(
                npc,
                Set.of(relevantParticipant),
                Set.of(MemoryEvent.Type.RELATIONSHIP_CHANGE),
                100L,
                100L,
                10,
                2
        );

        List<RankedMemory> ranked = MemoryRetriever.retrieve(store, query);

        assertEquals(relevantOld.id(), ranked.getFirst().event().id());
        assertEquals(100, ranked.getFirst().relevanceScore());
        assertEquals(50, ranked.getFirst().recencyScore());
        assertEquals(82, ranked.getFirst().totalScore());
        assertEquals(60, ranked.get(1).totalScore());
    }

    @Test
    void contextProviderFiltersForeignPlayerEventsBeforeCandidateLimit() {
        UUID npc = UUID.randomUUID();
        UUID currentPlayer = UUID.randomUUID();
        UUID foreignPlayer = UUID.randomUUID();
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

        store.append(eventWithSummary(
                UUID.randomUUID(), npc, MemoryEvent.Type.ACTION,
                Set.of(npc, currentPlayer), 1L, 20, 30, "eligible-current-player"
        ), 128);

        for (int i = 0; i < 32; i++) {
            store.append(eventWithSummary(
                    UUID.randomUUID(), npc, MemoryEvent.Type.RELATIONSHIP_CHANGE,
                    Set.of(npc, foreignPlayer), 100L + i, 100, 100, "foreign-" + i
            ), 128);
        }

        List<String> context = Memory2ContextProvider.load(tempDir, npc, currentPlayer, 200L);

        assertTrue(context.stream().anyMatch(line -> line.contains("eligible-current-player")));
        assertTrue(context.stream().noneMatch(line -> line.contains("foreign-")));
    }

    @Test
    void contextProviderKeepsNpcGlobalEventsVisibleToCurrentPlayer() {
        UUID npc = UUID.randomUUID();
        UUID currentPlayer = UUID.randomUUID();
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);
        store.append(eventWithSummary(
                UUID.randomUUID(), npc, MemoryEvent.Type.OBSERVATION,
                Set.of(npc), 100L, 80, 100, "npc-global-observation"
        ), 64);

        List<String> context = Memory2ContextProvider.load(tempDir, npc, currentPlayer, 200L);

        assertTrue(context.stream().anyMatch(line -> line.contains("npc-global-observation")));
    }

    @Test
    void contextProviderKeepsSharedEventVisibleWhenCurrentPlayerParticipates() {
        UUID npc = UUID.randomUUID();
        UUID currentPlayer = UUID.randomUUID();
        UUID otherEntity = UUID.randomUUID();
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);
        store.append(eventWithSummary(
                UUID.randomUUID(), npc, MemoryEvent.Type.ACTION,
                Set.of(npc, currentPlayer, otherEntity), 100L, 80, 100, "shared-current-player-event"
        ), 64);

        List<String> context = Memory2ContextProvider.load(tempDir, npc, currentPlayer, 200L);

        assertTrue(context.stream().anyMatch(line -> line.contains("shared-current-player-event")));
    }

    @Test
    void contextProviderExcludesForeignPlayerRelationshipCause() {
        UUID npc = UUID.randomUUID();
        UUID currentPlayer = UUID.randomUUID();
        UUID foreignPlayer = UUID.randomUUID();
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);
        store.append(eventWithSummary(
                UUID.randomUUID(), npc, MemoryEvent.Type.RELATIONSHIP_CAUSE,
                Set.of(npc, foreignPlayer), 100L, 100, 100, "foreign-causal-history"
        ), 64);

        List<String> context = Memory2ContextProvider.load(tempDir, npc, currentPlayer, 200L);

        assertTrue(context.stream().noneMatch(line -> line.contains("foreign-causal-history")));
    }

    @Test
    void contextProviderRecallsOldImportantEventAfterNewerEligibleWindowAndReload() throws Exception {
        Path firstWorld = tempDir.resolve("world-a");
        UUID npc = UUID.fromString("00000000-0000-0000-0000-000000000601");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000602");
        MemoryEventStore store = MemoryEventStore.forWorld(firstWorld);

        UUID durableId = UUID.fromString("00000000-0000-0000-0000-000000000603");
        MemoryEvent oldImportant = eventWithSummaryAndProvenance(
                durableId,
                npc,
                MemoryEvent.Type.RELATIONSHIP_CHANGE,
                Set.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                1L,
                100,
                100,
                "old-important-relationship"
        );
        store.append(oldImportant, 64);
        for (int i = 0; i < 40; i++) {
            store.append(eventWithSummaryAndProvenance(
                    new UUID(0L, 2_000L + i),
                    npc,
                    MemoryEvent.Type.DIALOGUE,
                    Set.of(npc, player),
                    MemoryEvent.Provenance.PLAYER_TOLD,
                    200_000L + i,
                    0,
                    0,
                    "new-weak-dialogue-" + i
            ), 64);
        }

        assertTrue(store.getRecent(npc, 64).stream().anyMatch(value -> value.id().equals(durableId)));

        Path secondWorld = tempDir.resolve("world-b");
        Path source = firstWorld.resolve("livingworld").resolve("memory2.json");
        Path target = secondWorld.resolve("livingworld").resolve("memory2.json");
        Files.createDirectories(target.getParent());
        Files.copy(source, target);

        List<String> context = Memory2ContextProvider.load(secondWorld, npc, player, 200_100L);

        assertTrue(context.stream().anyMatch(line -> line.contains("old-important-relationship")));
    }

    @Test
    void broadQueryTreatsAllCandidatesAsRelevant() {
        UUID npc = UUID.randomUUID();
        MemoryEventStore store = new MemoryEventStore(tempDir.resolve("memory2.json"));
        store.append(event(UUID.randomUUID(), npc, MemoryEvent.Type.ACTION, Set.of(), 10L, 50, 50), 10);

        RankedMemory ranked = MemoryRetriever.retrieve(
                store,
                new MemoryQuery(npc, Set.of(), Set.of(), 10L, 100L, 10, 1)
        ).getFirst();

        assertEquals(100, ranked.relevanceScore());
        assertEquals(100, ranked.recencyScore());
    }

    @Test
    void candidateAndResultLimitsAreHardBounds() {
        UUID npc = UUID.randomUUID();
        MemoryEventStore store = new MemoryEventStore(tempDir.resolve("memory2.json"));
        MemoryEvent oldImportant = event(UUID.randomUUID(), npc, MemoryEvent.Type.ACTION, Set.of(), 1L, 100, 100);
        MemoryEvent recentA = event(UUID.randomUUID(), npc, MemoryEvent.Type.ACTION, Set.of(), 100L, 1, 1);
        MemoryEvent recentB = event(UUID.randomUUID(), npc, MemoryEvent.Type.ACTION, Set.of(), 90L, 1, 1);
        store.append(oldImportant, 10);
        store.append(recentA, 10);
        store.append(recentB, 10);

        List<RankedMemory> ranked = MemoryRetriever.retrieve(
                store,
                new MemoryQuery(npc, Set.of(), Set.of(), 100L, 100L, 2, 1)
        );

        assertEquals(1, ranked.size());
        assertEquals(recentA.id(), ranked.getFirst().event().id());
    }

    @Test
    void tiesUseStableUuidOrdering() {
        UUID npc = UUID.randomUUID();
        MemoryEventStore store = new MemoryEventStore(tempDir.resolve("memory2.json"));
        MemoryEvent second = event(
                UUID.fromString("00000000-0000-0000-0000-000000000002"), npc,
                MemoryEvent.Type.OBSERVATION, Set.of(), 100L, 50, 50
        );
        MemoryEvent first = event(
                UUID.fromString("00000000-0000-0000-0000-000000000001"), npc,
                MemoryEvent.Type.OBSERVATION, Set.of(), 100L, 50, 50
        );
        store.append(second, 10);
        store.append(first, 10);

        List<RankedMemory> ranked = MemoryRetriever.retrieve(
                store,
                new MemoryQuery(npc, Set.of(), Set.of(), 100L, 100L, 10, 10)
        );

        assertEquals(List.of(first.id(), second.id()), ranked.stream().map(value -> value.event().id()).toList());
    }

    @Test
    void storeBackedRetrievalCannotLeakAnotherNpcsMemoryOrMutateStore() {
        UUID npcA = UUID.randomUUID();
        UUID npcB = UUID.randomUUID();
        MemoryEventStore store = new MemoryEventStore(tempDir.resolve("memory2.json"));
        MemoryEvent a = event(UUID.randomUUID(), npcA, MemoryEvent.Type.ACTION, Set.of(), 10L, 10, 10);
        MemoryEvent b = event(UUID.randomUUID(), npcB, MemoryEvent.Type.ACTION, Set.of(), 100L, 100, 100);
        store.append(a, 10);
        store.append(b, 10);
        List<MemoryEvent> before = store.getRecent(npcA, 10);

        List<RankedMemory> ranked = MemoryRetriever.retrieve(
                store,
                new MemoryQuery(npcA, Set.of(), Set.of(), 100L, 100L, 10, 10)
        );

        assertEquals(List.of(a.id()), ranked.stream().map(value -> value.event().id()).toList());
        assertEquals(before, store.getRecent(npcA, 10));
    }

    private static MemoryEvent event(
            UUID id,
            UUID owner,
            MemoryEvent.Type type,
            Set<UUID> participants,
            long gameTime,
            int importance,
            int confidence
    ) {
        return eventWithSummary(
                id,
                owner,
                type,
                participants,
                gameTime,
                importance,
                confidence,
                "memory-" + id
        );
    }

    private static MemoryEvent eventWithSummary(
            UUID id,
            UUID owner,
            MemoryEvent.Type type,
            Set<UUID> participants,
            long gameTime,
            int importance,
            int confidence,
            String summary
    ) {
        return eventWithSummaryAndProvenance(
                id,
                owner,
                type,
                participants,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                importance,
                confidence,
                summary
        );
    }

    private static MemoryEvent eventWithSummaryAndProvenance(
            UUID id,
            UUID owner,
            MemoryEvent.Type type,
            Set<UUID> participants,
            MemoryEvent.Provenance provenance,
            long gameTime,
            int importance,
            int confidence,
            String summary
    ) {
        return new MemoryEvent(
                id,
                owner,
                type,
                summary,
                List.copyOf(participants),
                provenance,
                gameTime,
                1_700_000_000_000L + gameTime,
                importance,
                0,
                confidence,
                List.of()
        );
    }
}
