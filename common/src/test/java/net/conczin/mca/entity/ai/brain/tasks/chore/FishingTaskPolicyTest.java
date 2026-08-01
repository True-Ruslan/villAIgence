package net.conczin.mca.entity.ai.brain.tasks.chore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingTaskPolicyTest {
    @Test
    void heldFishingRodAlwaysWins() {
        assertEquals(
                FishingTaskPolicy.RodSource.HELD,
                FishingTaskPolicy.rodSource(true, 4)
        );
    }

    @Test
    void inventoryRodIsEquippedWhenHandHasAnotherItem() {
        assertEquals(
                FishingTaskPolicy.RodSource.INVENTORY,
                FishingTaskPolicy.rodSource(false, 4)
        );
    }

    @Test
    void missingRodAbandonsFishing() {
        assertEquals(
                FishingTaskPolicy.RodSource.ABSENT,
                FishingTaskPolicy.rodSource(false, -1)
        );
    }

    @Test
    void emptyLootUsesDeterministicFallback() {
        assertTrue(FishingTaskPolicy.useFallbackLoot(0));
        assertTrue(FishingTaskPolicy.useFallbackLoot(-1));
        assertFalse(FishingTaskPolicy.useFallbackLoot(1));
    }

    @Test
    void selectedLootIndexIsBoundedAndStable() {
        assertEquals(0, FishingTaskPolicy.selectLootIndex(3, 0));
        assertEquals(2, FishingTaskPolicy.selectLootIndex(3, 2));
        assertEquals(1, FishingTaskPolicy.selectLootIndex(3, 4));
        assertEquals(2, FishingTaskPolicy.selectLootIndex(3, -1));
    }

    @Test
    void noIndexExistsForEmptyLoot() {
        assertEquals(-1, FishingTaskPolicy.selectLootIndex(0, 10));
    }
}
