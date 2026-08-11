package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PersonalitySocialSnapshotStoreViewTest {
    @TempDir
    Path tempDir;

    private static final UUID A = UUID.fromString("11111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID B = UUID.fromString("22222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void missingPairCaptureIsNeutralAndCreatesNoGraphFile() {
        Path graph = tempDir.resolve("livingworld/npc-social-graph.json");

        PersonalitySocialSnapshot snapshot = PersonalitySocialSnapshotStoreView.capture(
                tempDir,
                A,
                "friendly",
                B
        );

        assertEquals(A, snapshot.sourceNpcId());
        assertEquals(B, snapshot.counterpartNpcId());
        assertEquals(NpcSocialState.NEUTRAL, snapshot.directedSocialState());
        assertFalse(Files.exists(graph), "read-only capture must not create npc-social-graph.json");
    }

    @Test
    void existingGraphBytesRemainIdenticalAfterCaptureAndRender() throws Exception {
        NpcSocialGraphStore store = NpcSocialGraphStore.forWorld(tempDir);
        store.applyDelta(A, B, new NpcSocialDelta(7, -3, 2, 11), 20);
        Path graph = tempDir.resolve("livingworld/npc-social-graph.json");
        byte[] before = Files.readAllBytes(graph);

        PersonalitySocialSnapshot snapshot = PersonalitySocialSnapshotStoreView.capture(
                tempDir,
                A,
                "relaxed",
                B
        );
        PersonalitySocialContextRenderer.render(snapshot);

        assertEquals(new NpcSocialState(7, -3, 2, 11), snapshot.directedSocialState());
        assertArrayEquals(before, Files.readAllBytes(graph));
    }

    @Test
    void repeatedCaptureAndFreshRootReloadStayReadOnlyAndExact() throws Exception {
        NpcSocialGraphStore store = NpcSocialGraphStore.forWorld(tempDir);
        store.applyDelta(A, B, new NpcSocialDelta(15, -2, 6, 19), 20);
        Path graph = tempDir.resolve("livingworld/npc-social-graph.json");
        byte[] original = Files.readAllBytes(graph);

        for (int i = 0; i < 8; i++) {
            PersonalitySocialSnapshot snapshot = PersonalitySocialSnapshotStoreView.capture(
                    tempDir,
                    A,
                    "friendly",
                    B
            );
            assertEquals(new NpcSocialState(15, -2, 6, 19), snapshot.directedSocialState());
            PersonalitySocialContextRenderer.render(snapshot);
        }
        assertArrayEquals(original, Files.readAllBytes(graph));
        assertNoUnrelatedStateFiles(tempDir);

        Path freshRoot = tempDir.resolve("fresh-root");
        Path freshLivingWorld = freshRoot.resolve("livingworld");
        Files.createDirectories(freshLivingWorld);
        Path freshGraph = freshLivingWorld.resolve("npc-social-graph.json");
        Files.copy(graph, freshGraph, StandardCopyOption.REPLACE_EXISTING);
        byte[] freshBefore = Files.readAllBytes(freshGraph);

        PersonalitySocialSnapshot reloaded = PersonalitySocialSnapshotStoreView.capture(
                freshRoot,
                A,
                "friendly",
                B
        );

        assertEquals(new NpcSocialState(15, -2, 6, 19), reloaded.directedSocialState());
        assertArrayEquals(freshBefore, Files.readAllBytes(freshGraph));
        assertNoUnrelatedStateFiles(freshRoot);
    }

    @Test
    void existingUnrelatedStateFilesRemainByteIdenticalAfterCaptureAndRender() throws Exception {
        Path livingWorld = tempDir.resolve("livingworld");
        Files.createDirectories(livingWorld);
        Path relationships = livingWorld.resolve("relationships.json");
        Path memory2 = livingWorld.resolve("memory2.json");
        Path semanticMemory = livingWorld.resolve("semantic-memory.json");
        byte[] relationshipBytes = "{\"sentinel\":\"relationships\"}\n".getBytes(StandardCharsets.UTF_8);
        byte[] memory2Bytes = "{\"sentinel\":\"memory2\"}\n".getBytes(StandardCharsets.UTF_8);
        byte[] semanticBytes = "{\"sentinel\":\"semantic-memory\"}\n".getBytes(StandardCharsets.UTF_8);
        Files.write(relationships, relationshipBytes);
        Files.write(memory2, memory2Bytes);
        Files.write(semanticMemory, semanticBytes);

        NpcSocialGraphStore store = NpcSocialGraphStore.forWorld(tempDir);
        store.applyDelta(A, B, new NpcSocialDelta(4, 3, 2, 1), 20);

        for (int i = 0; i < 4; i++) {
            PersonalitySocialSnapshot snapshot = PersonalitySocialSnapshotStoreView.capture(
                    tempDir,
                    A,
                    "peaceful",
                    B
            );
            PersonalitySocialContextRenderer.render(snapshot);
        }

        assertArrayEquals(relationshipBytes, Files.readAllBytes(relationships));
        assertArrayEquals(memory2Bytes, Files.readAllBytes(memory2));
        assertArrayEquals(semanticBytes, Files.readAllBytes(semanticMemory));
    }

    @Test
    void directPairDirectionIsNeverInferredOrReversed() {
        NpcSocialGraphStore store = NpcSocialGraphStore.forWorld(tempDir);
        store.applyDelta(A, B, new NpcSocialDelta(9, 1, 0, 4), 20);
        store.applyDelta(B, A, new NpcSocialDelta(-6, 3, 8, -2), 20);

        PersonalitySocialSnapshot ab = PersonalitySocialSnapshotStoreView.capture(
                tempDir,
                A,
                "upbeat",
                B
        );
        PersonalitySocialSnapshot ba = PersonalitySocialSnapshotStoreView.capture(
                tempDir,
                B,
                "gloomy",
                A
        );

        assertEquals(new NpcSocialState(9, 1, 0, 4), ab.directedSocialState());
        assertEquals(new NpcSocialState(-6, 3, 8, -2), ba.directedSocialState());
    }

    @Test
    void noCounterpartDoesNotTouchGraphStore() {
        Path graph = tempDir.resolve("livingworld/npc-social-graph.json");

        PersonalitySocialSnapshot snapshot = PersonalitySocialSnapshotStoreView.capture(
                tempDir,
                A,
                "introverted",
                null
        );

        assertFalse(snapshot.hasCounterpart());
        assertEquals(NpcSocialState.NEUTRAL, snapshot.directedSocialState());
        assertFalse(Files.exists(graph));
    }

    private static void assertNoUnrelatedStateFiles(Path worldRoot) {
        Path livingWorld = worldRoot.resolve("livingworld");
        assertFalse(Files.exists(livingWorld.resolve("relationships.json")));
        assertFalse(Files.exists(livingWorld.resolve("memory2.json")));
        assertFalse(Files.exists(livingWorld.resolve("semantic-memory.json")));
    }
}
