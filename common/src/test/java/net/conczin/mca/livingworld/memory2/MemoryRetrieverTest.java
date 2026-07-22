package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        return new MemoryEvent(
                id,
                owner,
                type,
                "memory-" + id,
                List.copyOf(participants),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                1_700_000_000_000L,
                importance,
                0,
                confidence,
                List.of()
        );
    }
}
