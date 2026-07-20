package net.conczin.mca.livingworld.memory;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.List;

/** Loader-independent bridge from an MCA conversation to the world-local memory store. */
public final class PersistentChatMemory {
    private PersistentChatMemory() {
    }

    public static List<Tuple<String, String>> load(ServerPlayer player, VillagerEntityMCA villager) {
        return store(player).getMessages(villager.getUUID(), player.getUUID()).stream()
                .map(message -> new Tuple<>(message.role(), message.content()))
                .toList();
    }

    public static void append(
            ServerPlayer player,
            VillagerEntityMCA villager,
            String userMessage,
            String assistantMessage,
            LivingWorldConfig config
    ) {
        store(player).appendExchange(
                villager.getUUID(),
                player.getUUID(),
                userMessage,
                assistantMessage,
                config.persistentMemoryMaxMessages,
                config.persistentMemoryMaxCharsPerMessage
        );
    }

    private static ConversationMemoryStore store(ServerPlayer player) {
        Path worldRoot = player.serverLevel().getServer().getWorldPath(LevelResource.ROOT);
        return ConversationMemoryStore.forWorld(worldRoot);
    }
}
