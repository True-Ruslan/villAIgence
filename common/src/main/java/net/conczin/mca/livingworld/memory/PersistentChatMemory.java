package net.conczin.mca.livingworld.memory;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.memory2.WorkingMemoryContext;
import net.conczin.mca.livingworld.memory2.WorkingMemoryMessage;
import net.conczin.mca.livingworld.memory2.WorkingMemoryOrchestrator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Loader-independent bridge from an MCA conversation to the world-local memory store. */
public final class PersistentChatMemory {
    private PersistentChatMemory() {
    }

    public static List<Tuple<String, String>> load(ServerPlayer player, VillagerEntityMCA villager) {
        return load(worldRoot(player), villager.getUUID(), player.getUUID());
    }

    public static List<Tuple<String, String>> load(Path worldRoot, UUID villagerId, UUID playerId) {
        List<WorkingMemoryMessage> dialogue = ConversationMemoryStore.forWorld(worldRoot)
                .getMessages(villagerId, playerId)
                .stream()
                .filter(message -> message != null
                        && message.role() != null
                        && (message.role().equals("user") || message.role().equals("assistant"))
                        && message.content() != null
                        && !message.content().isBlank())
                .map(message -> new WorkingMemoryMessage(message.role(), message.content()))
                .toList();
        WorkingMemoryContext workingMemory = WorkingMemoryOrchestrator.compose(dialogue, List.of(), List.of());
        return workingMemory.recentDialogue().stream()
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
        append(worldRoot(player), villager.getUUID(), player.getUUID(), userMessage, assistantMessage, config);
    }

    public static void append(
            Path worldRoot,
            UUID villagerId,
            UUID playerId,
            String userMessage,
            String assistantMessage,
            LivingWorldConfig config
    ) {
        ConversationMemoryStore.forWorld(worldRoot).appendExchange(
                villagerId,
                playerId,
                userMessage,
                assistantMessage,
                config.persistentMemoryMaxMessages,
                config.persistentMemoryMaxCharsPerMessage
        );
    }

    private static Path worldRoot(ServerPlayer player) {
        return player.serverLevel().getServer().getWorldPath(LevelResource.ROOT);
    }
}
