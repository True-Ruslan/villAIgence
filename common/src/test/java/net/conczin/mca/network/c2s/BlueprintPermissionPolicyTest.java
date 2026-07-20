package net.conczin.mca.network.c2s;

import net.conczin.mca.resources.Rank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlueprintPermissionPolicyTest {
    @Test
    void mirrorsExistingRuleThresholds() {
        assertTrue(BlueprintPermissionPolicy.can(Rank.MERCHANT, BlueprintPermissionPolicy.Operation.CHANGE_TAXES));
        assertFalse(BlueprintPermissionPolicy.can(Rank.PEASANT, BlueprintPermissionPolicy.Operation.CHANGE_TAXES));

        assertTrue(BlueprintPermissionPolicy.can(Rank.NOBLE, BlueprintPermissionPolicy.Operation.CHANGE_POPULATION));
        assertFalse(BlueprintPermissionPolicy.can(Rank.MERCHANT, BlueprintPermissionPolicy.Operation.CHANGE_POPULATION));

        assertTrue(BlueprintPermissionPolicy.can(Rank.MAYOR, BlueprintPermissionPolicy.Operation.CHANGE_MARRIAGE));
        assertFalse(BlueprintPermissionPolicy.can(Rank.NOBLE, BlueprintPermissionPolicy.Operation.CHANGE_MARRIAGE));
    }

    @Test
    void destructiveAdministrationRequiresMayor() {
        for (BlueprintPermissionPolicy.Operation operation : new BlueprintPermissionPolicy.Operation[]{
                BlueprintPermissionPolicy.Operation.RENAME,
                BlueprintPermissionPolicy.Operation.REMOVE_BUILDING,
                BlueprintPermissionPolicy.Operation.FORCE_BUILDING_TYPE,
                BlueprintPermissionPolicy.Operation.TOGGLE_AUTO_SCAN,
                BlueprintPermissionPolicy.Operation.FULL_SCAN
        }) {
            assertTrue(BlueprintPermissionPolicy.can(Rank.MAYOR, operation), operation.name());
            assertFalse(BlueprintPermissionPolicy.can(Rank.NOBLE, operation), operation.name());
        }
    }

    @Test
    void normalLocalBuildingDiscoveryRemainsAvailableForProgression() {
        assertTrue(BlueprintPermissionPolicy.can(Rank.PEASANT, BlueprintPermissionPolicy.Operation.ADD_BUILDING));
        assertTrue(BlueprintPermissionPolicy.can(Rank.PEASANT, BlueprintPermissionPolicy.Operation.ADD_ROOM));
        assertFalse(BlueprintPermissionPolicy.can(Rank.OUTLAW, BlueprintPermissionPolicy.Operation.ADD_BUILDING));
    }

    @Test
    void validatesUntrustedValues() {
        assertTrue(BlueprintPermissionPolicy.isValidRatio(0.0f));
        assertTrue(BlueprintPermissionPolicy.isValidRatio(1.0f));
        assertFalse(BlueprintPermissionPolicy.isValidRatio(-0.01f));
        assertFalse(BlueprintPermissionPolicy.isValidRatio(1.01f));
        assertFalse(BlueprintPermissionPolicy.isValidRatio(Float.NaN));
        assertFalse(BlueprintPermissionPolicy.isValidRatio(Float.POSITIVE_INFINITY));

        assertEquals("Village Name", BlueprintPermissionPolicy.sanitizeName("  Village Name  "));
        assertEquals(32, BlueprintPermissionPolicy.sanitizeName("x".repeat(100)).length());
        assertEquals("", BlueprintPermissionPolicy.sanitizeName("   "));
    }
}
