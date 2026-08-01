package net.conczin.mca.mixin;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.ArcherControlPolicy;
import net.conczin.mca.entity.ai.ArcherMoveControl;
import net.conczin.mca.entity.ai.ArcherMoveControlOwner;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntityMCA.class)
abstract class MixinVillagerEntityMCA implements ArcherMoveControlOwner {
    @Unique
    private ArcherMoveControl mca$stableArcherMoveControl;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mca$captureArcherMoveControl(
            EntityType<VillagerEntityMCA> type,
            Level level,
            Gender gender,
            CallbackInfo ci
    ) {
        MoveControl activeControl = mca$getCurrentMoveControl();
        if (!(activeControl instanceof ArcherMoveControl archerMoveControl)) {
            throw new IllegalStateException(
                    "VillagerEntityMCA must be constructed with ArcherMoveControl"
            );
        }
        this.mca$stableArcherMoveControl = archerMoveControl;
    }

    @Override
    public ArcherMoveControl mca$getArcherMoveControl() {
        MoveControl activeControl = mca$getCurrentMoveControl();
        return ArcherControlPolicy.select(
                this.mca$stableArcherMoveControl,
                activeControl instanceof ArcherMoveControl active ? active : null
        );
    }

    @Unique
    private MoveControl mca$getCurrentMoveControl() {
        return ((Mob) (Object) this).getMoveControl();
    }

    @Redirect(
            method = "registerGoals",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V"
            )
    )
    private void mca$skipRedundantOpenDoorGoal(GoalSelector selector, int priority, Goal goal) {
        if (!(goal instanceof OpenDoorGoal)) {
            selector.addGoal(priority, goal);
        }
    }
}
