package net.conczin.mca.mixin;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VillagerEntityMCA.class)
abstract class MixinVillagerEntityMCA {
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
