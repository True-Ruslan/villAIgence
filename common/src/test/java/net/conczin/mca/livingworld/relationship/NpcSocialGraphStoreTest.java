package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSocialGraphStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsDirectedNpcPairsAcrossReload() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Path file = tempDir.resolve("npc-social-graph.json");

        NpcSocialGraphStore first = new NpcSocialGraphStore(file);
        assertEquals(
                NpcSocialGraphMutation.Status.APPLIED,
                first.applyDelta(a, b, new NpcSocialDelta(4, 2, 0, -1), 10).status()
        );
        assertEquals(
                NpcSocialGraphMutation.Status.APPLIED,
                first.applyDelta(b, a, new NpcSocialDelta(-3, 1, 2, 5), 10).status()
        );

        NpcSocialGraphStore reloaded = new NpcSocialGraphStore(file);
        assertEquals(new NpcSocialState(4, 2, 0, -1), reloaded.get(a, b));
        assertEquals(new NpcSocialState(-3, 1, 2, 5), reloaded.get(b, a));
    }

    @Test
    void selfEdgeFailsClosedWithoutMutation() {
        UUID npc = UUID.randomUUID();
        NpcSocialGraphStore store = new NpcSocialGraphStore(tempDir.resolve("npc-social-graph.json"));

        NpcSocialGraphMutation mutation = store.applyDelta(
                npc,
                npc,
                new NpcSocialDelta(5, 5, 5, 5),
                10
        );

        assertEquals(NpcSocialGraphMutation.Status.INVALID_PAIR, mutation.status());
        assertEquals(NpcSocialState.NEUTRAL, mutation.before());
        assertEquals(NpcSocialState.NEUTRAL, mutation.after());
        assertEquals(NpcSocialState.NEUTRAL, store.get(npc, npc));
    }

    @Test
    void nullPairFailsClosed() {
        UUID npc = UUID.randomUUID();
        NpcSocialGraphStore store = new NpcSocialGraphStore(tempDir.resolve("npc-social-graph.json"));

        assertEquals(
                NpcSocialGraphMutation.Status.INVALID_PAIR,
                store.applyDelta(null, npc, new NpcSocialDelta(1, 0, 0, 0), 10).status()
        );
        assertEquals(
                NpcSocialGraphMutation.Status.INVALID_PAIR,
                store.applyDelta(npc, null, new NpcSocialDelta(1, 0, 0, 0), 10).status()
        );
    }

    @Test
    void reportsExactBeforeAfterAndNoOpStatus() {
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        NpcSocialGraphStore store = new NpcSocialGraphStore(tempDir.resolve("npc-social-graph.json"));

        NpcSocialGraphMutation first = store.applyDelta(
                source,
                target,
                new NpcSocialDelta(5, 2, -1, 3),
                10
        );
        NpcSocialGraphMutation noOp = store.applyDelta(
                source,
                target,
                NpcSocialDelta.NONE,
                10
        );

        assertEquals(NpcSocialGraphMutation.Status.APPLIED, first.status());
        assertEquals(source, first.sourceNpcId());
        assertEquals(target, first.targetNpcId());
        assertEquals(NpcSocialState.NEUTRAL, first.before());
        assertEquals(new NpcSocialState(5, 2, -1, 3), first.after());
        assertTrue(first.changed());

        assertEquals(NpcSocialGraphMutation.Status.NO_CHANGE, noOp.status());
        assertEquals(first.after(), noOp.before());
        assertEquals(first.after(), noOp.after());
        assertFalse(noOp.changed());
    }

    @Test
    void returningToNeutralRemovesPersistedEdge() {
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Path file = tempDir.resolve("npc-social-graph.json");
        NpcSocialGraphStore store = new NpcSocialGraphStore(file);

        store.applyDelta(source, target, new NpcSocialDelta(3, -2, 1, 4), 10);
        NpcSocialGraphMutation removed = store.applyDelta(
                source,
                target,
                new NpcSocialDelta(-3, 2, -1, -4),
                10
        );

        assertEquals(NpcSocialGraphMutation.Status.APPLIED, removed.status());
        assertEquals(NpcSocialState.NEUTRAL, removed.after());
        assertEquals(NpcSocialState.NEUTRAL, store.get(source, target));
        assertEquals(NpcSocialState.NEUTRAL, new NpcSocialGraphStore(file).get(source, target));
    }
}
