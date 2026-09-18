package net.conczin.mca.network.c2s;

import net.conczin.mca.registry.ItemsMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BabyNamingAuthorityGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void forgedPacketCanOnlyTargetUnnamedBabyItems(GameTestHelper helper) {
        ItemStack ordinaryItem = new ItemStack(Items.DIAMOND);
        helper.assertTrue(
                !BabyNamingVillagerMessage.isNameableTarget(ordinaryItem),
                "Baby naming packet must not grant free CUSTOM_NAME edits to arbitrary inventory items"
        );

        ItemStack baby = new ItemStack(ItemsMCA.BABY_BOY);
        helper.assertTrue(
                BabyNamingVillagerMessage.isNameableTarget(baby),
                "Unnamed MCA baby item must remain a valid naming target"
        );

        ItemStack specializedBaby = new ItemStack(ItemsMCA.SIRBEN_BABY_GIRL);
        helper.assertTrue(
                BabyNamingVillagerMessage.isNameableTarget(specializedBaby),
                "Specialized BabyItem subclasses must remain valid naming targets"
        );

        baby.set(DataComponents.CUSTOM_NAME, Component.literal("Already named"));
        helper.assertTrue(
                !BabyNamingVillagerMessage.isNameableTarget(baby),
                "Forged packet must not rename a baby after the server-side naming flow has completed"
        );

        helper.succeed();
    }
}
