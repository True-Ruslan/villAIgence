package net.conczin.mca.mixin;

import net.conczin.mca.block.TombstoneBlock;
import net.conczin.mca.block.TombstoneDropPolicy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(TombstoneBlock.class)
abstract class MixinTombstoneBlock {
    @Inject(method = "getDrops", at = @At("RETURN"), cancellable = true, remap = false)
    private void mca$preserveFilledTombstoneDrop(
            BlockState state,
            LootParams.Builder builder,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        TombstoneBlock block = (TombstoneBlock)(Object)this;
        TombstoneBlock.Data.of(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY))
                .filter(TombstoneBlock.Data::hasEntity)
                .ifPresent(data -> cir.setReturnValue(TombstoneDropPolicy.ensurePreservedDrop(
                        cir.getReturnValue(),
                        stack -> stack.getItem() == block.asItem(),
                        () -> new ItemStack(block.asItem()),
                        data::writeToStack
                )));
    }
}
