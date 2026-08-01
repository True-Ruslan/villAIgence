package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.livingworld.lore.OperatorLoreScope;

/** Pure fail-closed target-resolution policy for untrusted client requests. */
public final class OperatorLoreTargetPolicy {
    public static final double MAX_DISTANCE_SQUARED = 64.0 * 64.0;

    private OperatorLoreTargetPolicy() {
    }

    public static boolean canResolve(
            OperatorLoreScope scope,
            boolean villagerPresent,
            boolean sameLevel,
            double distanceSquared,
            boolean homeVillagePresent
    ) {
        if (scope == null) {
            return false;
        }
        if (scope == OperatorLoreScope.WORLD || scope == OperatorLoreScope.PLAYER) {
            return true;
        }
        if (!villagerPresent
                || !sameLevel
                || !Double.isFinite(distanceSquared)
                || distanceSquared < 0.0
                || distanceSquared > MAX_DISTANCE_SQUARED) {
            return false;
        }
        return scope != OperatorLoreScope.VILLAGE || homeVillagePresent;
    }
}
