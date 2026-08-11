package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;
import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
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
}
