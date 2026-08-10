package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcSocialGraphCapacityTest {
    @TempDir
    Path tempDir;

    @Test
    void sixtyFifthOutgoingEdgeIsRejectedWithoutEviction() {
        UUID source = new UUID(1L, 1L);
        NpcSocialGraphStore store = store();
        List<UUID> retained = fillToCapacity(store, source);
        UUID overflow = new UUID(9L, 999L);

        NpcSocialGraphMutation rejected = store.applyDelta(
                source,
                overflow,
                new NpcSocialDelta(1, 0, 0, 0),
                10
        );

        assertEquals(NpcSocialGraphMutation.Status.CAPACITY_REACHED, rejected.status());
        assertEquals(NpcSocialState.NEUTRAL, rejected.before());
        assertEquals(NpcSocialState.NEUTRAL, rejected.after());
        assertEquals(NpcSocialState.NEUTRAL, store.get(source, overflow));
        assertEquals(new NpcSocialState(1, 0, 0, 0), store.get(source, retained.get(0)));
        assertEquals(new NpcSocialState(1, 0, 0, 0), store.get(source, retained.get(63)));
    }

    @Test
    void existingEdgeCanStillUpdateAtCapacity() {
        UUID source = new UUID(2L, 2L);
        NpcSocialGraphStore store = store();
        List<UUID> retained = fillToCapacity(store, source);
        UUID target = retained.get(17);

        NpcSocialGraphMutation mutation = store.applyDelta(
                source,
                target,
                new NpcSocialDelta(2, 1, 0, 0),
                10
        );

        assertEquals(NpcSocialGraphMutation.Status.APPLIED, mutation.status());
        assertEquals(new NpcSocialState(3, 1, 0, 0), mutation.after());
    }

    @Test
    void returningOneEdgeToNeutralFreesExactlyOneSlot() {
        UUID source = new UUID(3L, 3L);
        NpcSocialGraphStore store = store();
        List<UUID> retained = fillToCapacity(store, source);
        UUID removedTarget = retained.get(7);
        UUID replacement = new UUID(30L, 300L);
        UUID overflow = new UUID(31L, 301L);

        assertEquals(
                NpcSocialGraphMutation.Status.APPLIED,
                store.applyDelta(source, removedTarget, new NpcSocialDelta(-1, 0, 0, 0), 10).status()
        );
        assertEquals(NpcSocialState.NEUTRAL, store.get(source, removedTarget));
        assertEquals(
                NpcSocialGraphMutation.Status.APPLIED,
                store.applyDelta(source, replacement, new NpcSocialDelta(1, 0, 0, 0), 10).status()
        );
        assertEquals(
                NpcSocialGraphMutation.Status.CAPACITY_REACHED,
                store.applyDelta(source, overflow, new NpcSocialDelta(1, 0, 0, 0), 10).status()
        );
    }

    @Test
    void outgoingCapacityIsIsolatedPerSourceNpc() {
        UUID fullSource = new UUID(4L, 4L);
        UUID otherSource = new UUID(5L, 5L);
        UUID target = new UUID(40L, 400L);
        NpcSocialGraphStore store = store();
        fillToCapacity(store, fullSource);

        NpcSocialGraphMutation independent = store.applyDelta(
                otherSource,
                target,
                new NpcSocialDelta(1, 2, 3, 4),
                10
        );

        assertEquals(NpcSocialGraphMutation.Status.APPLIED, independent.status());
        assertEquals(new NpcSocialState(1, 2, 3, 4), independent.after());
    }

    private NpcSocialGraphStore store() {
        Path file = tempDir.resolve("npc-social-graph.json");
        return new NpcSocialGraphStore(file);
    }

    private static List<UUID> fillToCapacity(NpcSocialGraphStore store, UUID source) {
        List<UUID> targets = new ArrayList<>();
        for (int index = 0; index < 64; index++) {
            UUID target = new UUID(100L + index, 1000L + index);
            targets.add(target);
            assertEquals(
                    NpcSocialGraphMutation.Status.APPLIED,
                    store.applyDelta(source, target, new NpcSocialDelta(1, 0, 0, 0), 10).status(),
                    "edge " + index + " should be admitted"
            );
        }
        return targets;
    }
}
