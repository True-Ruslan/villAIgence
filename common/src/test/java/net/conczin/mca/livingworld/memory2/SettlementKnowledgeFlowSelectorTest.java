package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementKnowledgeFlowSelectorTest {
    @TempDir
    Path tempDir;

    @Test
    void selectionIsDeterministicAndHardBoundedBeforeOpportunityAllocation() {
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(tempDir.resolve("bounded"));
        List<UUID> residents = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            UUID resident = id(1_000 + i);
            residents.add(resident);
            for (int claim = 0; claim < 6; claim++) {
                store.append(belief(
                        id(10_000 + i * 100 + claim),
                        resident,
                        "resident " + i + " claim " + claim,
                        List.of(id(90_000 + i)),
                        100L + claim
                ), 64);
            }
        }
        residents.add(null);
        residents.add(residents.getFirst());

        SettlementKnowledgeFlowSelector.SelectionResult first =
                SettlementKnowledgeFlowSelector.select(store, 7, 2_400L, residents);
        SettlementKnowledgeFlowSelector.SelectionResult replay =
                SettlementKnowledgeFlowSelector.select(store, 7, 2_400L, residents);

        assertEquals(first, replay);
        assertEquals(SettlementKnowledgeFlowSelector.MAX_RESIDENTS_PER_CYCLE, first.residentWindow().size());
        assertEquals(SettlementKnowledgeFlowSelector.MAX_SPEAKERS_PER_CYCLE, first.speakersConsidered());
        assertTrue(first.opportunities().size() <= SettlementKnowledgeFlowSelector.MAX_OPPORTUNITIES_PER_CYCLE);
        assertEquals(first.residentWindow().size(), first.residentWindow().stream().distinct().count());

        for (SettlementKnowledgeFlowSelector.Opportunity opportunity : first.opportunities()) {
            assertTrue(first.residentWindow().contains(opportunity.speakerNpcId()));
            assertTrue(first.residentWindow().contains(opportunity.listenerNpcId()));
            assertFalse(opportunity.speakerNpcId().equals(opportunity.listenerNpcId()));

            List<UUID> newestTwo = store.getRecent(
                    opportunity.speakerNpcId(),
                    SettlementKnowledgeFlowSelector.MAX_SOURCE_CANDIDATES_PER_SPEAKER
            ).stream().map(SemanticMemoryEntry::id).toList();
            assertTrue(newestTwo.contains(opportunity.sourceSemanticEntryId()));
        }
    }

    @Test
    void selectedSourceCycleHasOneTargetAndNeverFallsBackAfterThatTargetLearnsClaim() {
        Path world = tempDir.resolve("no-fallback");
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        UUID speaker = id(1);
        UUID listenerA = id(2);
        UUID listenerB = id(3);
        UUID listenerC = id(4);
        List<UUID> residents = List.of(speaker, listenerA, listenerB, listenerC);

        SemanticMemoryEntry source = fact(
                id(100),
                speaker,
                "The bell is ringing",
                List.of(id(900)),
                200L
        );
        store.append(source, 64);

        SettlementKnowledgeFlowSelector.SelectionResult first =
                SettlementKnowledgeFlowSelector.select(store, 11, 3_600L, residents);
        SettlementKnowledgeFlowSelector.Opportunity opportunity = first.opportunities().stream()
                .filter(value -> value.sourceSemanticEntryId().equals(source.id()))
                .findFirst()
                .orElseThrow();

        store.append(belief(
                id(101),
                opportunity.listenerNpcId(),
                "  THE   BELL IS RINGING  ",
                List.of(id(900)),
                201L
        ), 64);

        SettlementKnowledgeFlowSelector.SelectionResult replay =
                SettlementKnowledgeFlowSelector.select(store, 11, 3_600L, residents);

        assertTrue(replay.opportunities().stream()
                .noneMatch(value -> value.sourceSemanticEntryId().equals(source.id())));
    }

    @Test
    void exactScopeMismatchDoesNotSuppressTheChosenTransfer() {
        Path world = tempDir.resolve("scope");
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        UUID speaker = id(20);
        UUID listener = id(21);
        List<UUID> residents = List.of(speaker, listener);

        SemanticMemoryEntry source = fact(id(200), speaker, "The bridge is safe", List.of(id(901)), 300L);
        store.append(source, 64);
        store.append(belief(id(201), listener, "The bridge is safe", List.of(id(902)), 301L), 64);

        SettlementKnowledgeFlowSelector.SelectionResult result =
                SettlementKnowledgeFlowSelector.select(store, 12, 4_800L, residents);

        assertTrue(result.opportunities().stream()
                .anyMatch(value -> value.sourceSemanticEntryId().equals(source.id())
                        && value.listenerNpcId().equals(listener)));
    }

    @Test
    void selectorCannotEmitResidentsOutsideTheSuppliedSettlementMembership() {
        Path world = tempDir.resolve("isolation");
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        UUID villageA1 = id(30);
        UUID villageA2 = id(31);
        UUID villageB = id(32);

        store.append(fact(id(300), villageA1, "Village A knows this", List.of(), 500L), 64);
        store.append(fact(id(301), villageB, "Village B knows this", List.of(), 500L), 64);

        List<UUID> villageAResidents = List.of(villageA1, villageA2);
        SettlementKnowledgeFlowSelector.SelectionResult result =
                SettlementKnowledgeFlowSelector.select(store, 100, 6_000L, villageAResidents);

        assertTrue(result.opportunities().stream().allMatch(value ->
                villageAResidents.contains(value.speakerNpcId())
                        && villageAResidents.contains(value.listenerNpcId())));
        assertTrue(result.opportunities().stream()
                .noneMatch(value -> value.speakerNpcId().equals(villageB)
                        || value.listenerNpcId().equals(villageB)));
    }

    private static SemanticMemoryEntry fact(
            UUID entryId,
            UUID npc,
            String statement,
            List<UUID> scope,
            long gameTime
    ) {
        return new SemanticMemoryEntry(
                entryId,
                npc,
                SemanticMemoryEntry.Kind.FACT,
                statement,
                scope,
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                90,
                100,
                List.of(id(700_000 + (int) gameTime))
        );
    }

    private static SemanticMemoryEntry belief(
            UUID entryId,
            UUID npc,
            String statement,
            List<UUID> scope,
            long gameTime
    ) {
        return new SemanticMemoryEntry(
                entryId,
                npc,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                scope,
                MemoryEvent.Provenance.PLAYER_TOLD,
                gameTime,
                0L,
                50,
                50,
                List.of(id(800_000 + (int) gameTime))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
