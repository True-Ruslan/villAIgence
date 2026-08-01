package net.conczin.mca.entity.ai.brain.tasks.chore;

/**
 * Loader-independent decisions used by the fishing chore.
 */
final class FishingTaskPolicy {
    private FishingTaskPolicy() {
    }

    enum RodSource {
        HELD,
        INVENTORY,
        ABSENT
    }

    static RodSource rodSource(boolean heldIsFishingRod, int inventoryRodSlot) {
        if (heldIsFishingRod) {
            return RodSource.HELD;
        }
        return inventoryRodSlot >= 0 ? RodSource.INVENTORY : RodSource.ABSENT;
    }

    static boolean useFallbackLoot(int lootSize) {
        return lootSize <= 0;
    }

    static int selectLootIndex(int lootSize, int randomIndex) {
        return lootSize <= 0 ? -1 : Math.floorMod(randomIndex, lootSize);
    }
}
