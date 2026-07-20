package net.conczin.mca.entity.ai.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathfindingProgressTrackerTest {
    @Test
    void triggersRecoveryAfterBoundedStationaryTicks() {
        PathfindingProgressTracker tracker = new PathfindingProgressTracker(3, 0.25D);

        assertFalse(tracker.update(10, 64, 10, 0.0D, 64.0D, 0.0D));
        assertFalse(tracker.update(10, 64, 10, 0.05D, 64.0D, 0.0D));
        assertFalse(tracker.update(10, 64, 10, 0.10D, 64.0D, 0.0D));
        assertTrue(tracker.update(10, 64, 10, 0.15D, 64.0D, 0.0D));
        assertFalse(tracker.update(10, 64, 10, 0.15D, 64.0D, 0.0D));
    }

    @Test
    void meaningfulProgressResetsStationaryCounter() {
        PathfindingProgressTracker tracker = new PathfindingProgressTracker(2, 0.25D);

        assertFalse(tracker.update(10, 64, 10, 0.0D, 64.0D, 0.0D));
        assertFalse(tracker.update(10, 64, 10, 0.0D, 64.0D, 0.0D));
        assertFalse(tracker.update(10, 64, 10, 0.5D, 64.0D, 0.0D));
        assertFalse(tracker.update(10, 64, 10, 0.5D, 64.0D, 0.0D));
        assertTrue(tracker.update(10, 64, 10, 0.5D, 64.0D, 0.0D));
    }

    @Test
    void changingTargetStartsFreshTrackingWindow() {
        PathfindingProgressTracker tracker = new PathfindingProgressTracker(2, 0.25D);

        assertFalse(tracker.update(10, 64, 10, 0.0D, 64.0D, 0.0D));
        assertFalse(tracker.update(10, 64, 10, 0.0D, 64.0D, 0.0D));
        assertFalse(tracker.update(20, 64, 20, 0.0D, 64.0D, 0.0D));
        assertFalse(tracker.update(20, 64, 20, 0.0D, 64.0D, 0.0D));
        assertTrue(tracker.update(20, 64, 20, 0.0D, 64.0D, 0.0D));
    }
}
