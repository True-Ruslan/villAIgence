package net.conczin.mca.entity.ai.navigation;

/**
 * Loader-independent collision scan boundary rules.
 */
final class NavigationCollisionPolicy {
    private static final double BOX_EPSILON = 1.0E-7D;

    private NavigationCollisionPolicy() {
    }

    static int floorMin(double value) {
        return (int)Math.floor(value);
    }

    static int floorMax(double value) {
        return (int)Math.floor(value - BOX_EPSILON);
    }

    static boolean isFullBlockCollision(boolean fullBlock) {
        return fullBlock;
    }

    static boolean requiresExactShapeCheck(boolean collisionShapeEmpty) {
        return !collisionShapeEmpty;
    }
}
