package net.conczin.mca.livingworld.knowledge;

import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.memory2.Memory2WorldEventIngestor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/** Records deterministic server-observed events after successful LivingWorld safe actions. */
public final class WorldEventRecorder {
    private WorldEventRecorder() {
    }

    /** Must be called on the Minecraft server thread after the action completed successfully. */
    public static void recordSuccessfulNpcAction(ServerPlayer player, VillagerEntityMCA villager, String commandName) {
        LivingWorldConfig config = LivingWorldConfig.getInstance();
        if (!config.isConfigured() || !config.eventMemoryEnabled) return;

        Optional<String> description = NpcActionEventFormatter.describe(
                commandName,
                villager.getName().getString(),
                player.getName().getString()
        );
        if (description.isEmpty()) return;

        Path worldRoot;
        WorldEvent event;
        try {
            BlockPos position = villager.blockPosition();
            worldRoot = player.serverLevel().getServer().getWorldPath(LevelResource.ROOT);
            event = new WorldEvent(
                    UUID.randomUUID(),
                    WorldEvent.Type.NPC_ACTION,
                    description.get(),
                    WorldEvent.Provenance.SYSTEM_OBSERVED,
                    villager.level().dimension().location().toString(),
                    position.getX(),
                    position.getY(),
                    position.getZ(),
                    villager.level().getGameTime(),
                    villager.getUUID(),
                    player.getUUID()
            );
            WorldEventStore.forWorld(worldRoot).append(event, config.eventMemoryMaxEvents);
        } catch (RuntimeException e) {
            MCA.LOGGER.warn(
                    "Unable to persist LivingWorld action event '{}' for villager {} and player {}",
                    commandName,
                    villager.getUUID(),
                    player.getUUID(),
                    e
            );
            return;
        }

        if (!config.memory2Enabled) return;
        try {
            Memory2WorldEventIngestor.record(
                    worldRoot,
                    event,
                    config.memory2MaxEventsPerNpc,
                    true,
                    config.memory2MaxEventsPerNpc,
                    System.currentTimeMillis()
            );
        } catch (RuntimeException e) {
            MCA.LOGGER.warn(
                    "Unable to finish LivingWorld action Memory 2.0 ingestion '{}' for villager {}",
                    commandName,
                    villager.getUUID(),
                    e
            );
        }
    }
}
