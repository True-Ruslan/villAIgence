package net.conczin.mca.entity.ai.navigation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

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
    void waterAwareNavigationPreservesVanillaPathPositionHook() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/navigation/MCAGroundPathNavigation.java"
        ));

        assertFalse(source.contains("protected Vec3 getTempMobPos("));
        assertTrue(source.contains("public int mca$getWaterAwareSurfaceY("));
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
