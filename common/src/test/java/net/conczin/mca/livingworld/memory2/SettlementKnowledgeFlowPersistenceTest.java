package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementKnowledgeFlowPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void freshRootReplayOfSameCycleCannotExpandFanout() throws IOException {
        Path sourceWorld = tempDir.resolve("source-world");
        Path reloadedWorld = tempDir.resolve("reloaded-world");
        UUID speaker = id(1);
        UUID listenerA = id(2);
        UUID listenerB = id(3);
        UUID listenerC = id(4);
        UUID sourceId = id(100);
        long cycleTime = 2_400L;
        List<UUID> residents = List.of(speaker, listenerA, listenerB, listenerC);

        SemanticMemoryStore.forWorld(sourceWorld).append(
                fact(sourceId, speaker, "The granary roof is damaged", List.of(), 100L), 64);
        SettlementKnowledgeFlowLifecycle.CycleResult first = SettlementKnowledgeFlowLifecycle.runCycle(
                sourceWorld, 17, cycleTime, residents, 64, 64);
        assertEquals(1, first.successfulTransfers(), first.toString());

        copyLivingworld(sourceWorld, reloadedWorld);
        long beforeEvidence = transferEvidenceCount(reloadedWorld, residents);
        long beforeListeners = listenerClaimCount(reloadedWorld, residents, speaker, "the granary roof is damaged");

        SettlementKnowledgeFlowLifecycle.CycleResult replay = SettlementKnowledgeFlowLifecycle.runCycle(
                reloadedWorld, 17, cycleTime, residents, 64, 64);

        assertEquals(0, replay.successfulTransfers(), replay.toString());
        assertEquals(beforeEvidence, transferEvidenceCount(reloadedWorld, residents));
        assertEquals(beforeListeners,
                listenerClaimCount(reloadedWorld, residents, speaker, "the granary roof is damaged"));
        assertEquals(1, beforeListeners);
    }

    @Test
    void laterCycleMayProgressToAnotherDeterministicTargetWithoutBroadcast() {
        Path world = tempDir.resolve("later-cycle");
        UUID speaker = id(10);
        UUID listenerA = id(11);
        UUID listenerB = id(12);
        UUID listenerC = id(13);
        UUID sourceId = id(110);
        List<UUID> residents = List.of(speaker, listenerA, listenerB, listenerC);
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        store.append(fact(sourceId, speaker, "The south field is flooded", List.of(), 100L), 64);

        long firstCycle = 2_400L;
        SettlementKnowledgeFlowSelector.Opportunity firstOpportunity = SettlementKnowledgeFlowSelector
                .select(store, 18, firstCycle, residents)
                .opportunities().stream()
                .filter(value -> value.sourceSemanticEntryId().equals(sourceId))
                .findFirst().orElseThrow();
        SettlementKnowledgeFlowLifecycle.CycleResult first = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 18, firstCycle, residents, 64, 64);
        assertEquals(1, first.successfulTransfers(), first.toString());

        Long laterCycle = null;
        UUID laterTarget = null;
        for (long cycle = firstCycle + SettlementKnowledgeFlowSelector.CYCLE_TICKS;
             cycle <= firstCycle + 12 * SettlementKnowledgeFlowSelector.CYCLE_TICKS;
             cycle += SettlementKnowledgeFlowSelector.CYCLE_TICKS) {
            var opportunity = SettlementKnowledgeFlowSelector.select(store, 18, cycle, residents)
                    .opportunities().stream()
                    .filter(value -> value.sourceSemanticEntryId().equals(sourceId))
                    .findFirst();
            if (opportunity.isPresent()
                    && !opportunity.get().listenerNpcId().equals(firstOpportunity.listenerNpcId())) {
                laterCycle = cycle;
                laterTarget = opportunity.get().listenerNpcId();
                break;
            }
        }
        assertNotNull(laterCycle);
        assertNotNull(laterTarget);

        SettlementKnowledgeFlowLifecycle.CycleResult later = SettlementKnowledgeFlowLifecycle.runCycle(
                world, 18, laterCycle, residents, 64, 64);
        assertEquals(1, later.successfulTransfers(), later.toString());
        assertEquals(2, listenerClaimCount(world, residents, speaker, "the south field is flooded"));
        assertTrue(SemanticMemoryStore.forWorld(world).getRecent(laterTarget, 64).stream()
                .anyMatch(entry -> SemanticMemoryIdentity.canonicalStatement(entry.statement())
                        .equals("the south field is flooded")));
    }

    @Test
    void twelveSettlementsRemainConstantBoundedAndMembershipIsolatedUnderHundredsOfClaims() {
        Path world = tempDir.resolve("pressure");
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        List<List<UUID>> villages = new ArrayList<>();

        for (int villageIndex = 0; villageIndex < 12; villageIndex++) {
            List<UUID> residents = new ArrayList<>();
            for (int residentIndex = 0; residentIndex < 24; residentIndex++) {
                int numeric = 1_000 + villageIndex * 100 + residentIndex;
                UUID resident = id(numeric);
                residents.add(resident);
                store.append(fact(
                        id(100_000 + numeric),
                        resident,
                        "Settlement " + villageIndex + " resident " + residentIndex + " report",
                        List.of(),
                        100L + residentIndex
                ), 64);
            }
            villages.add(List.copyOf(residents));
        }

        long cycleTime = 120_000L;
        for (int villageIndex = 0; villageIndex < villages.size(); villageIndex++) {
            List<UUID> residents = villages.get(villageIndex);
            Set<UUID> membership = new HashSet<>(residents);
            SettlementKnowledgeFlowSelector.SelectionResult result = SettlementKnowledgeFlowSelector.select(
                    store, 100 + villageIndex, cycleTime, residents);

            assertEquals(SettlementKnowledgeFlowSelector.MAX_RESIDENTS_PER_CYCLE,
                    result.residentWindow().size());
            assertEquals(SettlementKnowledgeFlowSelector.MAX_SPEAKERS_PER_CYCLE,
                    result.speakersConsidered());
            assertTrue(result.opportunities().size()
                    <= SettlementKnowledgeFlowSelector.MAX_OPPORTUNITIES_PER_CYCLE);
            assertTrue(result.residentWindow().stream().allMatch(membership::contains));
            assertTrue(result.opportunities().stream().allMatch(opportunity ->
                    membership.contains(opportunity.speakerNpcId())
                            && membership.contains(opportunity.listenerNpcId())));
        }
    }

    private static void copyLivingworld(Path sourceWorld, Path targetWorld) throws IOException {
        Path source = sourceWorld.resolve("livingworld");
        Path target = targetWorld.resolve("livingworld");
        Files.createDirectories(target);
        Files.copy(source.resolve("semantic-memory.json"), target.resolve("semantic-memory.json"),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(source.resolve("memory2.json"), target.resolve("memory2.json"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private static long transferEvidenceCount(Path world, List<UUID> residents) {
        return residents.stream()
                .flatMap(npc -> MemoryEventStore.forWorld(world).getRecent(npc, 64).stream())
                .filter(event -> event.type() == MemoryEvent.Type.DIALOGUE
                        && event.provenance() == MemoryEvent.Provenance.NPC_TOLD)
                .count();
    }

    private static long listenerClaimCount(
            Path world,
            List<UUID> residents,
            UUID speaker,
            String canonicalStatement
    ) {
        return residents.stream()
                .filter(npc -> !npc.equals(speaker))
                .filter(npc -> SemanticMemoryStore.forWorld(world).getRecent(npc, 64).stream()
                        .anyMatch(entry -> SemanticMemoryIdentity.canonicalStatement(entry.statement())
                                .equals(canonicalStatement)))
                .count();
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
                List.of(id((int) Math.floorMod(gameTime, 800_000L) + 900_000))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
