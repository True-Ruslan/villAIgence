package net.conczin.mca.mixin;

import net.conczin.mca.entity.ai.ArcherMoveControlOwner;
import net.conczin.mca.entity.ai.brain.tasks.ArcherMovementTask;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArcherMovementTask.class)
abstract class MixinArcherMovementTask {
    @Redirect(
            method = "getArcherMoveControl",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;getMoveControl()Lnet/minecraft/world/entity/ai/control/MoveControl;"
            )
    )
    private static MoveControl mca$useStableArcherMoveControl(Mob entity) {
        if (entity instanceof ArcherMoveControlOwner owner) {
            return owner.mca$getArcherMoveControl();
        }
        return entity.getMoveControl();
    }
}
