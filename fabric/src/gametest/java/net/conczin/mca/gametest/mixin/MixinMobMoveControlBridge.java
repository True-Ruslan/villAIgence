package net.conczin.mca.gametest.mixin;

import net.conczin.mca.gametest.bridge.MobMoveControlBridge;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Test-only Mixin bridge to the actual Mob.moveControl field. */
@Mixin(Mob.class)
abstract class MixinMobMoveControlBridge implements MobMoveControlBridge {
    @Shadow
    protected MoveControl moveControl;

    @Override
    public void mca$setActiveMoveControl(MoveControl replacement) {
        this.moveControl = replacement;
    }

    @Override
    public MoveControl mca$getActiveMoveControl() {
        return this.moveControl;
    }
}
