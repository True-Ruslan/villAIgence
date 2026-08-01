package net.conczin.mca.livingworld.lore.editor;

/** Pure permission and runtime-target gate used before opening the editor screen. */
public final class OperatorLoreEditorOpenPolicy {
    private OperatorLoreEditorOpenPolicy() {
    }

    public static boolean canOpenGlobal(boolean hasOperatorPermission) {
        return hasOperatorPermission;
    }

    public static boolean canOpenTargeted(
            boolean hasOperatorPermission,
            boolean villagerPresent,
            boolean sameLevel,
            double distanceSquared
    ) {
        return hasOperatorPermission
                && villagerPresent
                && sameLevel
                && Double.isFinite(distanceSquared)
                && distanceSquared >= 0.0
                && distanceSquared <= OperatorLoreTargetPolicy.MAX_DISTANCE_SQUARED;
    }
}
