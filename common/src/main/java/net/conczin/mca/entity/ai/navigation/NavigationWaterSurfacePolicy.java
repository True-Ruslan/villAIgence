package net.conczin.mca.entity.ai.navigation;

import java.util.function.IntPredicate;

/**
 * Loader-independent bounded water-surface scan used by MCA ground navigation.
 */
final class NavigationWaterSurfacePolicy {
    private NavigationWaterSurfacePolicy() {
    }

    static int findSurfaceY(
            boolean inWater,
            boolean canFloat,
            int startY,
            int fallbackY,
            IntPredicate isWaterAtY,
            int maxSteps
    ) {
        if (!inWater || !canFloat) {
            return fallbackY;
        }

        int surfaceY = startY;
        int steps = 0;
        while (isWaterAtY.test(surfaceY)) {
            surfaceY++;
            if (++steps > maxSteps) {
                return startY;
            }
        }
        return surfaceY;
    }
}
