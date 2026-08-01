package net.conczin.mca.entity.ai.brain.tasks;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathfindingSchedulePolicyTest {
    private static final int INTERVAL = 7;

    @Test
    void initialCooldownUsesDeterministicEntityOffset() {
        assertEquals(0, PathfindingSchedulePolicy.initialCooldown(0, INTERVAL));
        assertEquals(6, PathfindingSchedulePolicy.initialCooldown(6, INTERVAL));
        assertEquals(0, PathfindingSchedulePolicy.initialCooldown(7, INTERVAL));
        assertEquals(6, PathfindingSchedulePolicy.initialCooldown(-1, INTERVAL));
    }

    @Test
    void consecutiveEntityIdsCoverTheWholeInterval() {
        Set<Integer> offsets = new HashSet<>();
        for (int entityId = 100; entityId < 107; entityId++) {
            offsets.add(PathfindingSchedulePolicy.initialCooldown(entityId, INTERVAL));
        }

        assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6), offsets);
    }

    @Test
    void positiveCooldownSkipsAndDecrements() {
        PathfindingSchedulePolicy.Decision decision = PathfindingSchedulePolicy.tick(3, INTERVAL);

        assertFalse(decision.shouldRun());
        assertEquals(2, decision.nextCooldown());
    }

    @Test
    void zeroCooldownRunsAndResetsToIntervalMinusOne() {
        PathfindingSchedulePolicy.Decision decision = PathfindingSchedulePolicy.tick(0, INTERVAL);

        assertTrue(decision.shouldRun());
        assertEquals(6, decision.nextCooldown());
    }

    @Test
    void eachInitializedEntityRunsOncePerInterval() {
        for (int entityId = -10; entityId <= 10; entityId++) {
            int cooldown = PathfindingSchedulePolicy.initialCooldown(entityId, INTERVAL);
            int runs = 0;
            for (int tick = 0; tick < INTERVAL; tick++) {
                PathfindingSchedulePolicy.Decision decision = PathfindingSchedulePolicy.tick(cooldown, INTERVAL);
                cooldown = decision.nextCooldown();
                if (decision.shouldRun()) {
                    runs++;
                }
            }
            assertEquals(1, runs, "entityId=" + entityId);
        }
    }

    @Test
    void invalidIntervalsAreRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PathfindingSchedulePolicy.initialCooldown(1, 0)
        );
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PathfindingSchedulePolicy.tick(0, 0)
        );
    }
}
