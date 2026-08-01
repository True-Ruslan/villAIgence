package net.conczin.mca.entity.ai.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClimbNavigationPolicyTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void followingNodeDeterminesVerticalDirectionFirst() {
        assertEquals(1, ClimbNavigationPolicy.verticalDirection(10, 12, 8));
        assertEquals(-1, ClimbNavigationPolicy.verticalDirection(10, 8, 12));
    }

    @Test
    void previousNodeIsFallbackForFlatOrMissingFollowingNode() {
        assertEquals(1, ClimbNavigationPolicy.verticalDirection(10, 10, 8));
        assertEquals(-1, ClimbNavigationPolicy.verticalDirection(10, null, 12));
        assertEquals(0, ClimbNavigationPolicy.verticalDirection(10, null, null));
    }

    @Test
    void ascentAndDescentUseDirectionalReachedChecks() {
        assertTrue(ClimbNavigationPolicy.hasReachedHeight(9.81D, 10.0D, 1, 0.20D));
        assertFalse(ClimbNavigationPolicy.hasReachedHeight(9.79D, 10.0D, 1, 0.20D));
        assertTrue(ClimbNavigationPolicy.hasReachedHeight(10.08D, 10.0D, -1, 0.08D));
        assertFalse(ClimbNavigationPolicy.hasReachedHeight(10.09D, 10.0D, -1, 0.08D));
    }

    @Test
    void verticalVelocityCannotReverseTheIntendedDirection() {
        assertEquals(0.16D, ClimbNavigationPolicy.verticalVelocity(0.8D, 1, 0.16D), EPSILON);
        assertEquals(0.0D, ClimbNavigationPolicy.verticalVelocity(-0.8D, 1, 0.16D), EPSILON);
        assertEquals(-0.16D, ClimbNavigationPolicy.verticalVelocity(-0.8D, -1, 0.16D), EPSILON);
        assertEquals(0.0D, ClimbNavigationPolicy.verticalVelocity(0.8D, -1, 0.16D), EPSILON);
    }

    @Test
    void exitBiasKeepsMotionPointingOutOfTheClimbable() {
        assertEquals(0.08D, ClimbNavigationPolicy.applyExitBias(0.0D, 1, true, 0.08D), EPSILON);
        assertEquals(-0.08D, ClimbNavigationPolicy.applyExitBias(0.0D, -1, true, 0.08D), EPSILON);
        assertEquals(0.03D, ClimbNavigationPolicy.applyExitBias(0.03D, 1, false, 0.08D), EPSILON);
    }

    @Test
    void horizontalVelocityIsProportionalAndBounded() {
        assertEquals(0.12D, ClimbNavigationPolicy.horizontalVelocity(2.0D, 0.35D, 0.12D), EPSILON);
        assertEquals(-0.12D, ClimbNavigationPolicy.horizontalVelocity(-2.0D, 0.35D, 0.12D), EPSILON);
        assertEquals(0.07D, ClimbNavigationPolicy.horizontalVelocity(0.2D, 0.35D, 0.12D), EPSILON);
    }

    @Test
    void jumpIsSuppressedOnlyWhileNavigationControlsClimb() {
        assertFalse(ClimbNavigationPolicy.allowJump(true, true));
        assertTrue(ClimbNavigationPolicy.allowJump(true, false));
        assertFalse(ClimbNavigationPolicy.allowJump(false, false));
    }

    @Test
    void followDistanceAccountsForVerticalSeparationAndClimbableState() {
        assertEquals(0, ClimbNavigationPolicy.followCloseEnoughDistance(3, false));
        assertEquals(2, ClimbNavigationPolicy.followCloseEnoughDistance(1, false));
        assertEquals(1, ClimbNavigationPolicy.followCloseEnoughDistance(3, true));
    }
}
