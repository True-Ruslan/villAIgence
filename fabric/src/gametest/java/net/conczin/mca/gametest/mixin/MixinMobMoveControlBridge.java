package net.conczin.mca.gametest.mixin;

import net.conczin.mca.gametest.bridge.MobMoveControlBridge;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only accessor to the actual Mob.moveControl field. */
@Mixin(Mob.class)
interface MixinMobMoveControlBridge extends MobMoveControlBridge {
    @Override
    @Accessor("moveControl")
    void mca$setActiveMoveControl(MoveControl replacement);

    @Override
    @Accessor("moveControl")
    MoveControl mca$getActiveMoveControl();
}
