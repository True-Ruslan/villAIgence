package net.conczin.mca.entity.ai.brain.tasks;

/**
 * Loader-independent deterministic scheduling policy for expensive pathfinding checks.
 */
final class PathfindingSchedulePolicy {
    private PathfindingSchedulePolicy() {
    }

    static int initialCooldown(int entityId, int interval) {
        requirePositiveInterval(interval);
        return Math.floorMod(entityId, interval);
    }

    static Decision tick(int cooldown, int interval) {
        requirePositiveInterval(interval);
        if (cooldown > 0) {
            return new Decision(cooldown - 1, false);
        }
        return new Decision(interval - 1, true);
    }

    private static void requirePositiveInterval(int interval) {
        if (interval < 1) {
            throw new IllegalArgumentException("interval must be positive");
        }
    }

    record Decision(int nextCooldown, boolean shouldRun) {
    }
}
