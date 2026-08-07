package net.conczin.mca.livingworld.memory;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.memory2.Memory2DialogueHistory;
import net.conczin.mca.livingworld.memory2.WorkingMemoryMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Compatibility facade for the existing ChatAI call surface.
 *
 * <p>Persistent dialogue is sourced exclusively from Memory 2.0. Successful persistent writes are
 * owned by {@code ChatAI -> Memory2DialogueLifecycle}; the append methods intentionally perform no
 * second write so provider retries and the compatibility facade cannot duplicate events.</p>
 */
public final class PersistentChatMemory {
    private PersistentChatMemory() {
    }

    public static List<Tuple<String, String>> load(ServerPlayer player, VillagerEntityMCA villager) {
        return load(worldRoot(player), villager.getUUID(), player.getUUID());
    }

    public static List<Tuple<String, String>> load(Path worldRoot, UUID villagerId, UUID playerId) {
        return Memory2DialogueHistory.load(worldRoot, villagerId, playerId).stream()
                .map(PersistentChatMemory::toTuple)
                .toList();
    }

    public static void append(
            ServerPlayer player,
            VillagerEntityMCA villager,
            String userMessage,
            String assistantMessage,
            LivingWorldConfig config
    ) {
        // Intentionally empty. ChatAI persists the successful turn exactly once via Memory 2.0.
    }

    public static void append(
            Path worldRoot,
            UUID villagerId,
            UUID playerId,
            String userMessage,
            String assistantMessage,
            LivingWorldConfig config
    ) {
        // Intentionally empty. ChatAI persists the successful turn exactly once via Memory 2.0.
    }

    private static Tuple<String, String> toTuple(WorkingMemoryMessage message) {
        return new Tuple<>(message.role(), message.content());
    }

    private static Path worldRoot(ServerPlayer player) {
        return player.serverLevel().getServer().getWorldPath(LevelResource.ROOT);
    }
}
