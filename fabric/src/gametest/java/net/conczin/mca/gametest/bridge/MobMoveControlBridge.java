package net.conczin.mca.gametest.bridge;

import net.minecraft.world.entity.ai.control.MoveControl;

/** Test-only bridge for reproducing a vehicle-owned active controller replacement. */
public interface MobMoveControlBridge {
    void mca$setActiveMoveControl(MoveControl replacement);

    MoveControl mca$getActiveMoveControl();
}
