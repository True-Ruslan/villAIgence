package net.conczin.mca.entity.ai.brain.tasks.chore;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.registry.EntitiesMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FishingCatchGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void selectedCatchTransfersLootAndDamagesActualHeldRodExactlyOnce(
            GameTestHelper helper
    ) {
        VillagerEntityMCA villager = helper.spawn(
                EntitiesMCA.MALE_VILLAGER,
                2,
                1,
                2
        );
        ItemStack rod = new ItemStack(Items.FISHING_ROD);
        villager.setItemInHand(villager.getDominantHand(), rod);

        FishingCatchApplication.apply(villager, new ItemStack(Items.COD, 2));

        helper.assertTrue(count(villager, Items.COD) == 2,
                "One selected catch must transfer exactly two cod into MCA inventory");
        helper.assertTrue(villager.getItemInHand(villager.getDominantHand()) == rod,
                "Fishing catch must damage the actual held rod instead of a detached copy");
        helper.assertTrue(rod.getDamageValue() == 1,
                "One selected catch must damage the held rod exactly once, found "
                        + rod.getDamageValue());

        helper.succeed();
    }

    private static int count(VillagerEntityMCA villager, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < villager.getInventory().getContainerSize(); slot++) {
            ItemStack stack = villager.getInventory().getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
