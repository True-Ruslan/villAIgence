package net.conczin.mca.entity.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResidencyBedClaimPolicyTest {
    @Test
    void unoccupiedBedIsClaimable() {
        assertTrue(ResidencyBedClaimPolicy.isClaimableBed(true, true, false));
    }

    @Test
    void occupiedBedIsRejected() {
        assertFalse(ResidencyBedClaimPolicy.isClaimableBed(true, true, true));
    }

    @Test
    void nonBedIsRejected() {
        assertFalse(ResidencyBedClaimPolicy.isClaimableBed(false, true, false));
    }

    @Test
    void stateWithoutOccupiedPropertyIsRejected() {
        assertFalse(ResidencyBedClaimPolicy.isClaimableBed(true, false, false));
    }

    @Test
    void reclaimingSameHomeDoesNotReleaseItsPoiTicket() {
        assertFalse(ResidencyBedClaimPolicy.shouldReleasePreviousHome(true, true, true));
    }

    @Test
    void changingPositionReleasesPreviousHome() {
        assertTrue(ResidencyBedClaimPolicy.shouldReleasePreviousHome(true, true, false));
    }

    @Test
    void changingDimensionReleasesPreviousHome() {
        assertTrue(ResidencyBedClaimPolicy.shouldReleasePreviousHome(true, false, true));
    }

    @Test
    void absentPreviousHomeRequiresNoRelease() {
        assertFalse(ResidencyBedClaimPolicy.shouldReleasePreviousHome(false, false, false));
    }
}
