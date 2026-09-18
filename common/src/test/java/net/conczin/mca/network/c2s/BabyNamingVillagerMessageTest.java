package net.conczin.mca.network.c2s;

import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.item.BabyItem;
import net.conczin.mca.item.SirbenBabyItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BabyNamingVillagerMessageTest {
    @Test
    void rejectsArbitraryInventoryItemsAsNamingTargets() {
        ItemStack ordinaryItem = new ItemStack(new Item(new Item.Properties()));

        assertFalse(BabyNamingVillagerMessage.isNameableTarget(ordinaryItem));
    }

    @Test
    void acceptsOnlyBabyItemsIncludingSpecializedBabies() {
        ItemStack baby = new ItemStack(new BabyItem(Gender.MALE, new Item.Properties()));
        ItemStack sirbenBaby = new ItemStack(new SirbenBabyItem(Gender.FEMALE, new Item.Properties()));

        assertTrue(BabyNamingVillagerMessage.isNameableTarget(baby));
        assertTrue(BabyNamingVillagerMessage.isNameableTarget(sirbenBaby));
    }
}
