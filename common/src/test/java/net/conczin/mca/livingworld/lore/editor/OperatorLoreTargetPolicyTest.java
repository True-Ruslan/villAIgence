package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.livingworld.lore.OperatorLoreScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorLoreTargetPolicyTest {
    @Test
    void worldAndPlayerScopesNeverTrustAClientTarget() {
        assertTrue(OperatorLoreTargetPolicy.canResolve(
                OperatorLoreScope.WORLD,
                false,
                false,
                Double.POSITIVE_INFINITY,
                false
        ));
        assertTrue(OperatorLoreTargetPolicy.canResolve(
                OperatorLoreScope.PLAYER,
                false,
                false,
                Double.POSITIVE_INFINITY,
                false
        ));
    }

    @Test
    void villagerScopeRequiresNearbyLiveEntityInSameLevel() {
        assertTrue(OperatorLoreTargetPolicy.canResolve(
                OperatorLoreScope.VILLAGER,
                true,
                true,
                OperatorLoreTargetPolicy.MAX_DISTANCE_SQUARED,
                false
        ));
        assertFalse(OperatorLoreTargetPolicy.canResolve(
                OperatorLoreScope.VILLAGER,
                false,
                true,
                0.0,
                false
        ));
        assertFalse(OperatorLoreTargetPolicy.canResolve(
                OperatorLoreScope.VILLAGER,
                true,
                false,
                0.0,
                false
        ));
        assertFalse(OperatorLoreTargetPolicy.canResolve(
                OperatorLoreScope.VILLAGER,
                true,
                true,
                OperatorLoreTargetPolicy.MAX_DISTANCE_SQUARED + 0.01,
                false
        ));
    }

    @Test
    void villageScopeAlsoRequiresResolvedHomeVillage() {
        assertTrue(OperatorLoreTargetPolicy.canResolve(
                OperatorLoreScope.VILLAGE,
                true,
                true,
                1.0,
                true
        ));
        assertFalse(OperatorLoreTargetPolicy.canResolve(
                OperatorLoreScope.VILLAGE,
                true,
                true,
                1.0,
                false
        ));
    }

    @Test
    void malformedDistanceAlwaysFailsClosed() {
        assertFalse(OperatorLoreTargetPolicy.canResolve(
                OperatorLoreScope.VILLAGER,
                true,
                true,
                Double.NaN,
                false
        ));
        assertFalse(OperatorLoreTargetPolicy.canResolve(
                OperatorLoreScope.VILLAGER,
                true,
                true,
                -1.0,
                false
        ));
    }
}
