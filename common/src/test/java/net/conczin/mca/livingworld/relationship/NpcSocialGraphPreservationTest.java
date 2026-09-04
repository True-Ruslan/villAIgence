package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcSocialGraphPreservationTest {
    @TempDir
    Path tempDir;

    @Test
    void multiNpcPressureRemainsDirectedRestartSafeAndIndependentFromPlayerRelationships() throws Exception {
        Path worldRoot = tempDir.resolve("world-a");
        Path livingWorld = worldRoot.resolve("livingworld");
        Files.createDirectories(livingWorld);

        UUID relationshipNpc = new UUID(9000L, 9000L);
        UUID relationshipPlayer = new UUID(9001L, 9001L);
        LivingWorldRelationshipStore playerRelationships =
                new LivingWorldRelationshipStore(livingWorld.resolve("relationships.json"));
        playerRelationships.applyDelta(
                relationshipNpc,
                relationshipPlayer,
                new LivingWorldRelationshipDelta(7, 3, -2, 5),
                10
        );
        byte[] relationshipsBeforeGraph = Files.readAllBytes(
                livingWorld.resolve("relationships.json")
        );

        NpcSocialGraphStore graph = new NpcSocialGraphStore(
                livingWorld.resolve("npc-social-graph.json")
        );
        List<EdgeExpectation> expected = new ArrayList<>();
        for (int sourceIndex = 0; sourceIndex < 12; sourceIndex++) {
            UUID source = new UUID(100L + sourceIndex, 1000L + sourceIndex);
            for (int targetIndex = 0; targetIndex < 8; targetIndex++) {
                UUID target = new UUID(
                        10000L + sourceIndex * 100L + targetIndex,
                        20000L + sourceIndex * 100L + targetIndex
                );
                NpcSocialState state = new NpcSocialState(
                        sourceIndex - targetIndex,
                        targetIndex + 1,
                        sourceIndex % 4,
                        targetIndex - sourceIndex
                );
                NpcSocialGraphMutation mutation = graph.applyDelta(
                        source,
                        target,
                        new NpcSocialDelta(
                                state.trust(),
                                state.respect(),
                                state.fear(),
                                state.affinity()
                        ),
                        100
                );
                assertEquals(NpcSocialGraphMutation.Status.APPLIED, mutation.status());
                expected.add(new EdgeExpectation(source, target, state));
                assertEquals(
                        NpcSocialState.NEUTRAL,
                        graph.get(target, source),
                        "directed reverse edge must remain independent"
                );
            }
        }

        assertArrayEquals(
                relationshipsBeforeGraph,
                Files.readAllBytes(livingWorld.resolve("relationships.json")),
                "NPC graph writes must not mutate the NPC×player relationship store"
        );

        Path freshRoot = tempDir.resolve("world-b");
        Path freshLivingWorld = freshRoot.resolve("livingworld");
        Files.createDirectories(freshLivingWorld);
        Files.copy(
                livingWorld.resolve("npc-social-graph.json"),
                freshLivingWorld.resolve("npc-social-graph.json"),
                StandardCopyOption.REPLACE_EXISTING
        );

        NpcSocialGraphStore reloaded = new NpcSocialGraphStore(
                freshLivingWorld.resolve("npc-social-graph.json")
        );
        for (EdgeExpectation edge : expected) {
            assertEquals(edge.state(), reloaded.get(edge.source(), edge.target()));
            assertEquals(NpcSocialState.NEUTRAL, reloaded.get(edge.target(), edge.source()));
        }

        assertEquals(
                new LivingWorldRelationshipState(7, 3, -2, 5),
                playerRelationships.get(relationshipNpc, relationshipPlayer)
        );
    }

    @Test
    void freshRootReloadRebuildsOutgoingCapacityIndexAtTheExactSameBoundary() throws Exception {
        Path worldRoot = tempDir.resolve("capacity-world-a");
        Path livingWorld = worldRoot.resolve("livingworld");
        Files.createDirectories(livingWorld);

        UUID atCapacitySource = new UUID(7000L, 7000L);
        UUID oneUnderCapacitySource = new UUID(7001L, 7001L);
        NpcSocialGraphStore graph = new NpcSocialGraphStore(
                livingWorld.resolve("npc-social-graph.json")
        );

        for (int index = 0; index < 64; index++) {
            UUID target = new UUID(8000L + index, 80000L + index);
            assertEquals(
                    NpcSocialGraphMutation.Status.APPLIED,
                    graph.applyDelta(atCapacitySource, target, new NpcSocialDelta(1, 0, 0, 0), 10).status()
            );
        }
        for (int index = 0; index < 63; index++) {
            UUID target = new UUID(9000L + index, 90000L + index);
            assertEquals(
                    NpcSocialGraphMutation.Status.APPLIED,
                    graph.applyDelta(oneUnderCapacitySource, target, new NpcSocialDelta(1, 0, 0, 0), 10).status()
            );
        }
        // Churn: retire one edge and re-admit a different one so the persisted edge count
        // for atCapacitySource stays 64 but no longer matches the original target set.
        UUID retiredTarget = new UUID(8000L, 80000L);
        UUID replacementTarget = new UUID(8100L, 81000L);
        assertEquals(
                NpcSocialGraphMutation.Status.APPLIED,
                graph.applyDelta(atCapacitySource, retiredTarget, new NpcSocialDelta(-1, 0, 0, 0), 10).status()
        );
        assertEquals(
                NpcSocialGraphMutation.Status.APPLIED,
                graph.applyDelta(atCapacitySource, replacementTarget, new NpcSocialDelta(1, 0, 0, 0), 10).status()
        );

        Path freshRoot = tempDir.resolve("capacity-world-b");
        Path freshLivingWorld = freshRoot.resolve("livingworld");
        Files.createDirectories(freshLivingWorld);
        Files.copy(
                livingWorld.resolve("npc-social-graph.json"),
                freshLivingWorld.resolve("npc-social-graph.json"),
                StandardCopyOption.REPLACE_EXISTING
        );

        NpcSocialGraphStore reloaded = new NpcSocialGraphStore(
                freshLivingWorld.resolve("npc-social-graph.json")
        );

        assertEquals(NpcSocialState.NEUTRAL, reloaded.get(atCapacitySource, retiredTarget));
        assertEquals(new NpcSocialState(1, 0, 0, 0), reloaded.get(atCapacitySource, replacementTarget));

        UUID rejectedOverflow = new UUID(8200L, 82000L);
        assertEquals(
                NpcSocialGraphMutation.Status.CAPACITY_REACHED,
                reloaded.applyDelta(atCapacitySource, rejectedOverflow, new NpcSocialDelta(1, 0, 0, 0), 10).status(),
                "reloaded index must reject the 65th edge exactly like the live incremental index did"
        );

        UUID admittedThe64th = new UUID(9100L, 91000L);
        assertEquals(
                NpcSocialGraphMutation.Status.APPLIED,
                reloaded.applyDelta(oneUnderCapacitySource, admittedThe64th, new NpcSocialDelta(1, 0, 0, 0), 10).status(),
                "reloaded index must admit the 64th edge for a source that persisted at exactly 63"
        );
        UUID rejectedThe65th = new UUID(9200L, 92000L);
        assertEquals(
                NpcSocialGraphMutation.Status.CAPACITY_REACHED,
                reloaded.applyDelta(oneUnderCapacitySource, rejectedThe65th, new NpcSocialDelta(1, 0, 0, 0), 10).status()
        );
    }

    private record EdgeExpectation(UUID source, UUID target, NpcSocialState state) {
    }
}
