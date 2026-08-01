package net.conczin.mca.mixin;

import net.conczin.mca.block.TombstoneBlock;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.brain.tasks.MourningMemoryLifecycle;
import net.conczin.mca.entity.ai.relationship.CompassionateEntity;
import net.conczin.mca.entity.ai.relationship.EntityRelationship;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TombstoneBlock.Data.class)
abstract class MixinTombstoneData {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/conczin/mca/entity/ai/relationship/CompassionateEntity;getRelationships()Lnet/conczin/mca/entity/ai/relationship/EntityRelationship;",
                    remap = false
            ),
            remap = false
    )
    private EntityRelationship mca$clearMourningAfterResurrection(
            CompassionateEntity<?> compassionateEntity
    ) {
        if (compassionateEntity instanceof VillagerEntityMCA villager) {
            MourningMemoryLifecycle.clearAfterResurrection(villager);
        }
        return compassionateEntity.getRelationships();
    }
}
