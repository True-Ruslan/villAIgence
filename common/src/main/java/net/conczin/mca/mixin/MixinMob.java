package net.conczin.mca.mixin;

import net.conczin.mca.entity.VillagerConversionSupport;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
abstract class MixinMob {
    @Shadow
    protected abstract float getEquipmentDropChance(EquipmentSlot slot);

    @Inject(
            method = "convertTo(Lnet/minecraft/world/entity/EntityType;Z)Lnet/minecraft/world/entity/Mob;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T extends Mob> void mca$preserveVillagerUuid(
            EntityType<T> targetType,
            boolean keepEquipment,
            CallbackInfoReturnable<T> cir
    ) {
        Mob source = (Mob) (Object) this;
        if (!VillagerConversionSupport.shouldPreserveUuid(source, targetType)) {
            return;
        }

        cir.setReturnValue(VillagerConversionSupport.convertPreservingUuid(
                source,
                targetType,
                keepEquipment,
                this::getEquipmentDropChance
        ));
    }
}
