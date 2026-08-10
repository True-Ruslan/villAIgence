package net.conczin.mca.mixin;

import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.memory2.SettlementKnowledgeFlowRuntime;
import net.conczin.mca.server.world.data.Village;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reuses the existing staggered loaded-village update as the settlement gossip cadence. */
@Mixin(Village.class)
public abstract class MixinVillage {
    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/conczin/mca/server/world/data/villageComponents/VillageGuardsManager;spawnGuards(Lnet/minecraft/server/level/ServerLevel;)V"
            )
    )
    private void vai$runSettlementKnowledgeFlow(
            ServerLevel world,
            long scheduledTime,
            CallbackInfo callbackInfo
    ) {
        Village village = (Village) (Object) this;
        LivingWorldConfig config = LivingWorldConfig.getInstance();
        SettlementKnowledgeFlowRuntime.runIfEnabled(
                config.enabled && config.memory2Enabled,
                world.getServer().getWorldPath(LevelResource.ROOT),
                village.getId(),
                world.getGameTime(),
                village.getResidentsUUIDs().toList(),
                config.memory2MaxEventsPerNpc
        );
    }
}
