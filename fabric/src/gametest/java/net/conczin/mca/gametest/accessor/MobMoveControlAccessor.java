package net.conczin.mca.gametest.accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only accessor for reproducing a vehicle-owned active controller replacement. */
@Mixin(Mob.class)
public interface MobMoveControlAccessor {
    @Accessor("moveControl")
    void mca$setActiveMoveControl(MoveControl replacement);
}
