package net.conczin.mca.entity.ai.chatAI;

import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class TriggerCommandInfo {
    public String command;
    public String description;
    public BiPredicate<ServerPlayer, VillagerEntityMCA> isActive;
    public BiConsumer<ServerPlayer, VillagerEntityMCA> call;

    public TriggerCommandInfo(String command, String description, BiConsumer<ServerPlayer, VillagerEntityMCA> call, BiPredicate<ServerPlayer, VillagerEntityMCA> isActive) {
        this.command = command;
        this.description = description;
        this.isActive = isActive;
        this.call = (player, villager) -> player.serverLevel().getServer().execute(() -> {
            if (!player.isAlive() || villager.isRemoved() || player.level() != villager.level()) return;
            if (this.isActive != null && !this.isActive.test(player, villager)) return;
            try {
                call.accept(player, villager);
            } catch (RuntimeException e) {
                MCA.LOGGER.warn("AI action '{}' failed for villager {} and player {}", command, villager.getUUID(), player.getUUID(), e);
            }
        });
    }

    public TriggerCommandInfo(String command, String description, BiConsumer<ServerPlayer, VillagerEntityMCA> call) {
        this(command, description, call, (p, v) -> true);
    }
}
