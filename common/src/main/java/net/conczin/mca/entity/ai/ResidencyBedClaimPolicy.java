package net.conczin.mca.entity.ai;

/**
 * Loader-independent HOME bed selection and previous-claim release policy.
 */
final class ResidencyBedClaimPolicy {
    private ResidencyBedClaimPolicy() {
    }

    static boolean isClaimableBed(boolean taggedBed, boolean hasOccupiedProperty, boolean occupied) {
        return taggedBed && hasOccupiedProperty && !occupied;
    }

    static boolean shouldReleasePreviousHome(
            boolean previousHomePresent,
            boolean sameDimension,
            boolean samePosition
    ) {
        return previousHomePresent && !(sameDimension && samePosition);
    }
}
