package net.conczin.mca.livingworld.actions;

import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.chatAI.TriggerCommandInfos;
import net.minecraft.server.level.ServerPlayer;

/** Executes only MCA's hard-coded AI command whitelist and always mutates world state on the server thread. */
public final class SafeActionExecutor {
    private SafeActionExecutor() {
    }

    public static void execute(ServerPlayer player, VillagerEntityMCA villager, String commandName) {
        if (commandName == null || commandName.isBlank()) return;
        player.serverLevel().getServer().execute(() -> {
            if (!player.isAlive() || villager.isRemoved() || player.level() != villager.level()) return;
            TriggerCommandInfos.findCommand(commandName, player, villager)
                    .ifPresent(command -> {
                        try {
                            command.call.accept(player, villager);
                        } catch (RuntimeException e) {
                            MCA.LOGGER.warn("LivingWorld safe action '{}' failed for villager {} and player {}", commandName, villager.getUUID(), player.getUUID(), e);
                        }
                    });
        });
    }
}
