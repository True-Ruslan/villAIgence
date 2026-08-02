package net.conczin.mca.gametest;

import net.conczin.mca.block.TombstoneBlock;
import net.conczin.mca.registry.BlocksMCA;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class TombstoneControlGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void emptyTombstoneSilkTouchDoesNotSynthesizeNpcData(GameTestHelper helper) {
        TombstoneBlock tombstoneBlock = (TombstoneBlock) BlocksMCA.UPRIGHT_HEADSTONE;
        BlockState tombstoneState = tombstoneBlock.defaultBlockState();
        TombstoneBlock.Data emptyData = new TombstoneBlock.Data(
                helper.absolutePos(new BlockPos(2, 1, 2)),
                tombstoneState
        );

        ItemStack silkTouchPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        silkTouchPickaxe.enchant(
                helper.getLevel()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.SILK_TOUCH),
                1
        );

        LootParams.Builder lootBuilder = new LootParams.Builder(helper.getLevel())
                .withParameter(
                        LootContextParams.ORIGIN,
                        Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)))
                )
                .withParameter(LootContextParams.TOOL, silkTouchPickaxe)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, emptyData);

        List<ItemStack> drops = tombstoneBlock.getDrops(tombstoneState, lootBuilder);
        long tombstoneCount = drops.stream()
                .filter(stack -> stack.is(tombstoneBlock.asItem()))
                .count();

        helper.assertTrue(tombstoneCount <= 1,
                "Empty Silk Touch grave must not duplicate tombstone items");
        helper.assertTrue(drops.stream().noneMatch(stack -> stack.has(DataComponents.BLOCK_ENTITY_DATA)),
                "Empty grave drops must not contain synthesized NPC block-entity data");
        helper.succeed();
    }
}
