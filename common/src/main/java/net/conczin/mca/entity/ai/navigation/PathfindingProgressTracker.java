package net.conczin.mca.entity.ai.navigation;

/**
 * Tracks whether an entity is making meaningful progress towards the same walk target.
 * This class is intentionally Minecraft-independent so the recovery policy can be unit tested.
 */
public final class PathfindingProgressTracker {
    private final int stuckThresholdTicks;
    private final double minProgressSquared;

    private boolean initialized;
    private int targetX;
    private int targetY;
    private int targetZ;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private int stationaryTicks;

    public PathfindingProgressTracker(int stuckThresholdTicks, double minProgress) {
        if (stuckThresholdTicks < 1) throw new IllegalArgumentException("stuckThresholdTicks must be positive");
        if (minProgress <= 0.0D) throw new IllegalArgumentException("minProgress must be positive");
        this.stuckThresholdTicks = stuckThresholdTicks;
        this.minProgressSquared = minProgress * minProgress;
    }

    /**
     * @return true once when recovery should be triggered. The internal counter resets after triggering.
     */
    public boolean update(int targetX, int targetY, int targetZ, double x, double y, double z) {
        if (!initialized || targetChanged(targetX, targetY, targetZ)) {
            reset(targetX, targetY, targetZ, x, y, z);
            return false;
        }

        double dx = x - anchorX;
        double dy = y - anchorY;
        double dz = z - anchorZ;
        if (dx * dx + dy * dy + dz * dz >= minProgressSquared) {
            anchorX = x;
            anchorY = y;
            anchorZ = z;
            stationaryTicks = 0;
            return false;
        }

        stationaryTicks++;
        if (stationaryTicks >= stuckThresholdTicks) {
            anchorX = x;
            anchorY = y;
            anchorZ = z;
            stationaryTicks = 0;
            return true;
        }
        return false;
    }

    public void clear() {
        initialized = false;
        stationaryTicks = 0;
    }

    private boolean targetChanged(int x, int y, int z) {
        return targetX != x || targetY != y || targetZ != z;
    }

    private void reset(int targetX, int targetY, int targetZ, double x, double y, double z) {
        initialized = true;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        anchorX = x;
        anchorY = y;
        anchorZ = z;
        stationaryTicks = 0;
    }
}
