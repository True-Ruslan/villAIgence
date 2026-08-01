package net.conczin.mca.entity.ai;

/**
 * Loader-independent rule for retaining the MCA archer controller when a vehicle
 * temporarily replaces the entity's active move controller.
 */
public final class ArcherControlPolicy {
    private ArcherControlPolicy() {
    }

    public static <T> T select(T stableControl, T activeControl) {
        if (stableControl == null) {
            throw new IllegalStateException("MCA archer control was not captured during construction");
        }
        return stableControl;
    }
}
