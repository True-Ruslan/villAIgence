package net.conczin.mca.entity.ai.brain.tasks.chore;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Applies one already-selected fishing catch to server-owned villager state. */
final class FishingCatchApplication {
    private FishingCatchApplication() {
    }

    static void apply(VillagerEntityMCA entity, ItemStack caught) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(caught, "caught");
        if (caught.isEmpty()) {
            throw new IllegalArgumentException("caught fishing loot must not be empty");
        }

        ItemStack heldRod = entity.getItemInHand(entity.getDominantHand());
        if (!(heldRod.getItem() instanceof FishingRodItem)) {
            throw new IllegalStateException(
                    "fishing catch requires the actual dominant-hand fishing rod"
            );
        }

        entity.getInventory().addItem(caught);
        heldRod.hurtAndBreak(1, entity, entity.getDominantSlot());
    }
}
