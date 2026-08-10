package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedSemanticContradictionProducerPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void remainsHardBoundedAcrossTenNpcPressureSimulation() {
        Path world = tempDir.resolve("multi-npc-pressure");

        for (int npcIndex = 0; npcIndex < 10; npcIndex++) {
            UUID npc = id(1_000 + npcIndex);
            UUID player = id(2_000 + npcIndex);
            SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);

            for (int candidateIndex = 0; candidateIndex < 24; candidateIndex++) {
                store.append(belief(
                        id(10_000 + npcIndex * 100 + candidateIndex),
                        npc,
                        "Distinct pressure claim " + npcIndex + "-" + candidateIndex,
                        List.of(npc, player),
                        100L + candidateIndex
                ), 64);
            }

            SemanticMemoryEntry subject = fact(
                    id(20_000 + npcIndex),
                    npc,
                    "Subject pressure claim " + npcIndex,
                    List.of(npc, player),
                    10_000L + npcIndex
            );
            store.append(subject, 64);

            BoundedSemanticContradictionProducer.ProductionResult result =
                    BoundedSemanticContradictionProducer.produce(world, subject, 64);

            assertEquals(SemanticContradictionCandidateSelector.MAX_CANDIDATES_PER_ADMISSION,
                    result.eligibleCandidates());
            assertEquals(SemanticContradictionCandidateSelector.MAX_COMPARISONS_PER_ADMISSION,
                    result.comparisons());
            assertEquals(0, result.oppositions());
            assertTrue(result.recordedEventIds().isEmpty());
        }
    }

    @Test
    void forgottenClaimsStopResolvingEvenWhenContradictionEvidenceSurvives() {
        Path world = tempDir.resolve("forgetting");
        UUID npc = id(30_001);
        UUID player = id(30_002);

        ControlledSemanticMemoryIngestor.recordFact(
                world,
                observed(id(30_100), npc, player, "The gate is open", 100L),
                64
        );
        ControlledSemanticMemoryIngestor.recordBelief(
                world,
                new SemanticBeliefSource(
                        npc,
                        "The gate is not open",
                        List.of(npc, player),
                        MemoryEvent.Provenance.PLAYER_TOLD,
                        200L,
                        0L,
                        50,
                        50,
                        List.of(id(30_101))
                ),
                64
        );

        assertEquals(1, SemanticContradictionHistory.load(world, npc, player, 8).size());
        assertEquals(1, MemoryEventStore.forWorld(world).getRecentMatching(
                npc,
                64,
                event -> event.type() == MemoryEvent.Type.SEMANTIC_CONTRADICTION
                        && SemanticContradictionPolicy.valid(event)
        ).size());

        SemanticMemoryStore.forWorld(world).append(
                fact(
                        id(30_200),
                        npc,
                        "Much newer retained observation",
                        List.of(npc, player),
                        1_000_000_000L
                ),
                1
        );

        assertTrue(SemanticContradictionHistory.load(world, npc, player, 8).isEmpty());
        assertEquals(1, MemoryEventStore.forWorld(world).getRecentMatching(
                npc,
                64,
                event -> event.type() == MemoryEvent.Type.SEMANTIC_CONTRADICTION
                        && SemanticContradictionPolicy.valid(event)
        ).size());
    }

    @Test
    void freshRootReloadPreservesLiveRelationWithoutDuplicatingClaimProse() throws IOException {
        Path source = tempDir.resolve("source");
        Path reloaded = tempDir.resolve("reloaded");
        UUID npc = id(40_001);
        UUID player = id(40_002);

        ControlledSemanticMemoryIngestor.recordFact(
                source,
                observed(id(40_100), npc, player, "The gate is open", 100L),
                64
        );
        ControlledSemanticMemoryIngestor.recordBelief(
                source,
                new SemanticBeliefSource(
                        npc,
                        "The gate is not open",
                        List.of(npc, player),
                        MemoryEvent.Provenance.NPC_TOLD,
                        200L,
                        0L,
                        50,
                        50,
                        List.of(id(40_101))
                ),
                64
        );

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> before =
                SemanticContradictionHistory.load(source, npc, player, 8);
        assertEquals(1, before.size());

        Path sourceLivingworld = source.resolve("livingworld");
        Path targetLivingworld = reloaded.resolve("livingworld");
        Files.createDirectories(targetLivingworld);
        Files.copy(
                sourceLivingworld.resolve("semantic-memory.json"),
                targetLivingworld.resolve("semantic-memory.json"),
                StandardCopyOption.REPLACE_EXISTING
        );
        Files.copy(
                sourceLivingworld.resolve("memory2.json"),
                targetLivingworld.resolve("memory2.json"),
                StandardCopyOption.REPLACE_EXISTING
        );

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> after =
                SemanticContradictionHistory.load(reloaded, npc, player, 8);

        assertEquals(before, after);
        List<String> statements = List.of(
                after.getFirst().first().statement(),
                after.getFirst().second().statement()
        );
        assertTrue(statements.contains("The gate is open"));
        assertTrue(statements.contains("The gate is not open"));
        assertFalse(after.getFirst().evidence().summary().contains("The gate is open"));
        assertFalse(after.getFirst().evidence().summary().contains("The gate is not open"));
    }

    private static MemoryEvent observed(
            UUID eventId,
            UUID npc,
            UUID player,
            String summary,
            long gameTime
    ) {
        return new MemoryEvent(
                eventId,
                npc,
                MemoryEvent.Type.OBSERVATION,
                summary,
                List.of(npc, player),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                gameTime,
                0L,
                90,
                0,
                100,
                List.of()
        );
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
                100,
                100,
                List.of(id((int) Math.floorMod(gameTime, 900_000L) + 50_000))
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
                MemoryEvent.Provenance.NPC_TOLD,
                gameTime,
                0L,
                50,
                50,
                List.of(id((int) Math.floorMod(gameTime, 900_000L) + 950_000))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
