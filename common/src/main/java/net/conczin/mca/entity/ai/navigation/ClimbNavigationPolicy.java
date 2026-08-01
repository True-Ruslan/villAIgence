package net.conczin.mca.entity.ai.navigation;

/**
 * Loader-independent directional motion policy for climbable navigation.
 */
public final class ClimbNavigationPolicy {
    private ClimbNavigationPolicy() {
    }

    static int verticalDirection(int climbableY, Integer followingY, Integer previousY) {
        if (followingY != null) {
            int direction = Integer.compare(followingY, climbableY);
            if (direction != 0) {
                return direction;
            }
        }
        return previousY == null ? 0 : Integer.compare(climbableY, previousY);
    }

    static boolean hasReachedHeight(double currentY, double targetY, int verticalDirection, double tolerance) {
        if (verticalDirection > 0) {
            return currentY >= targetY - tolerance;
        }
        if (verticalDirection < 0) {
            return currentY <= targetY + tolerance;
        }
        return Math.abs(targetY - currentY) <= tolerance;
    }

    static double verticalVelocity(double delta, int verticalDirection, double maxSpeed) {
        if (verticalDirection > 0) {
            return clamp(delta, 0.0D, maxSpeed);
        }
        if (verticalDirection < 0) {
            return clamp(delta, -maxSpeed, 0.0D);
        }
        return clamp(delta, -maxSpeed, maxSpeed);
    }

    static double applyExitBias(double velocity, int verticalDirection, boolean atExitHeight, double bias) {
        if (!atExitHeight || verticalDirection == 0) {
            return velocity;
        }
        return verticalDirection > 0
                ? Math.max(velocity, bias)
                : Math.min(velocity, -bias);
    }

    static double horizontalVelocity(double delta, double gain, double maxSpeed) {
        return clamp(delta * gain, -maxSpeed, maxSpeed);
    }

    public static boolean allowJump(boolean requested, boolean navigationControlsClimb) {
        return requested && !navigationControlsClimb;
    }

    public static int followCloseEnoughDistance(int verticalDistance, boolean onClimbable) {
        if (onClimbable) {
            return 1;
        }
        return verticalDistance > 1 ? 0 : 2;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
