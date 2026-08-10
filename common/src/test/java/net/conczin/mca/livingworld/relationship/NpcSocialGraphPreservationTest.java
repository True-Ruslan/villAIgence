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

    private record EdgeExpectation(UUID source, UUID target, NpcSocialState state) {
    }
}
