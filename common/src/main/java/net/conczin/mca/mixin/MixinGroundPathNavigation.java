package net.conczin.mca.mixin;

import net.conczin.mca.entity.ai.navigation.MCAGroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GroundPathNavigation.class)
abstract class MixinGroundPathNavigation {
    @Inject(method = "getSurfaceY", at = @At("HEAD"), cancellable = true)
    private void mca$useWaterTagAwareSurface(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this instanceof MCAGroundPathNavigation navigation) {
            cir.setReturnValue(navigation.mca$getWaterAwareSurfaceY());
        }
    }
}
