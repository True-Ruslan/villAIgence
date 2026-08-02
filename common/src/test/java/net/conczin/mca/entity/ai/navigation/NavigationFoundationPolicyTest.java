package net.conczin.mca.entity.ai.navigation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationFoundationPolicyTest {
    @Test
    void waterSurfaceStopsAtFirstNonWaterBlock() {
        Set<Integer> waterLayers = Set.of(40, 41, 42);

        int surfaceY = NavigationWaterSurfacePolicy.findSurfaceY(
                true,
                true,
                40,
                40,
                waterLayers::contains,
                16
        );

        assertEquals(43, surfaceY);
    }

    @Test
    void waterSurfaceSearchIsBounded() {
        int surfaceY = NavigationWaterSurfacePolicy.findSurfaceY(
                true,
                true,
                40,
                40,
                y -> true,
                16
        );

        assertEquals(40, surfaceY);
    }

    @Test
    void nonFloatingMobUsesVanillaFallbackHeight() {
        int surfaceY = NavigationWaterSurfacePolicy.findSurfaceY(
                true,
                false,
                40,
                57,
                y -> true,
                16
        );

        assertEquals(57, surfaceY);
    }

    @Test
    void waterAwareNavigationPreservesVanillaPathPositionHook() {
        Set<String> declaredMethods = Arrays.stream(MCAGroundPathNavigation.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertFalse(declaredMethods.contains("getTempMobPos"));
        assertTrue(declaredMethods.contains("mca$getWaterAwareSurfaceY"));
    }

    @Test
    void exactIntegerMaximumDoesNotIncludeTheNextBlock() {
        assertEquals(4, NavigationCollisionPolicy.floorMin(4.0D));
        assertEquals(4, NavigationCollisionPolicy.floorMax(5.0D));
    }

    @Test
    void fractionalMaximumIncludesItsContainingBlock() {
        assertEquals(5, NavigationCollisionPolicy.floorMax(5.25D));
    }

    @Test
    void fullBlockRejectsClearanceImmediately() {
        assertTrue(NavigationCollisionPolicy.isFullBlockCollision(true));
        assertFalse(NavigationCollisionPolicy.isFullBlockCollision(false));
    }

    @Test
    void onlyNonEmptyPartialShapeRequiresExactCollisionFallback() {
        assertTrue(NavigationCollisionPolicy.requiresExactShapeCheck(false));
        assertFalse(NavigationCollisionPolicy.requiresExactShapeCheck(true));
    }
}
