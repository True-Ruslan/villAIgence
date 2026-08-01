package net.conczin.mca.block;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TombstoneEntityDataCodecTest {
    @Test
    void writesBlockEntityDataAndDefensivelyCopiesThePayload() {
        ItemStack stack = new ItemStack(Items.CHEST);
        CompoundTag payload = tombstonePayload("Pio");

        TombstoneBlock.Data.writeEntityTag(stack, BlockEntityType.CHEST, payload);
        payload.putString("EntityName", "mutated-after-write");

        CompoundTag stored = TombstoneBlock.Data.readEntityTag(stack).orElseThrow();
        assertEquals("Pio", stored.getString("EntityName"));
        assertEquals("minecraft:chest", stored.getString("id"));
        assertTrue(stack.has(DataComponents.BLOCK_ENTITY_DATA));
        assertFalse(stack.has(DataComponents.ENTITY_DATA));
    }

    @Test
    void readsLegacyEntityDataWhenBlockEntityDataIsAbsent() {
        ItemStack stack = new ItemStack(Items.CHEST);
        stack.set(DataComponents.ENTITY_DATA, CustomData.of(tombstonePayload("legacy")));

        CompoundTag stored = TombstoneBlock.Data.readEntityTag(stack).orElseThrow();

        assertEquals("legacy", stored.getString("EntityName"));
    }

    @Test
    void blockEntityDataTakesPrecedenceOverLegacyEntityData() {
        ItemStack stack = new ItemStack(Items.CHEST);
        stack.set(DataComponents.ENTITY_DATA, CustomData.of(tombstonePayload("legacy")));
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tombstonePayload("current")));

        CompoundTag stored = TombstoneBlock.Data.readEntityTag(stack).orElseThrow();

        assertEquals("current", stored.getString("EntityName"));
    }

    @Test
    void missingComponentsReturnEmpty() {
        assertTrue(TombstoneBlock.Data.readEntityTag(new ItemStack(Items.CHEST)).isEmpty());
        assertTrue(TombstoneBlock.Data.readEntityTag(null).isEmpty());
    }

    @Test
    void readReturnsADefensiveCopy() {
        ItemStack stack = new ItemStack(Items.CHEST);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tombstonePayload("Pio")));

        CompoundTag firstRead = TombstoneBlock.Data.readEntityTag(stack).orElseThrow();
        firstRead.putString("EntityName", "mutated-read");

        CompoundTag secondRead = TombstoneBlock.Data.readEntityTag(stack).orElseThrow();
        assertEquals("Pio", secondRead.getString("EntityName"));
    }

    private static CompoundTag tombstonePayload(String name) {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "mca:villager");

        CompoundTag payload = new CompoundTag();
        payload.put("EntityData", entity);
        payload.putString("EntityName", name);
        payload.putInt("EntityGender", 0);
        return payload;
    }
}
