package net.conczin.mca.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import net.conczin.mca.entity.ai.brain.VillagerTasksMCA;
import net.conczin.mca.entity.ai.brain.tasks.MourningTaskPackage;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerTasksMCA.class)
abstract class MixinVillagerTasksMCA {
    @Shadow(remap = false)
    @Final
    @Mutable
    private static ImmutableList<MemoryModuleType<?>> MEMORY_TYPES;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void mca$appendMourningMemories(CallbackInfo ci) {
        MEMORY_TYPES = ImmutableList.<MemoryModuleType<?>>builder()
                .addAll(MEMORY_TYPES)
                .add(MemoryModuleTypeMCA.MOURNING_SITE)
                .add(MemoryModuleTypeMCA.MOURNING_POSITION)
                .build();
    }

    @Inject(method = "getGrievingPackage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mca$useStableMourningPackage(
            CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super VillagerEntityMCA>>>> cir
    ) {
        cir.setReturnValue(MourningTaskPackage.create());
    }
}
