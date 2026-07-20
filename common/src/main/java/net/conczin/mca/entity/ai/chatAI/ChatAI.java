package net.conczin.mca.entity.ai.chatAI;

import net.conczin.mca.Config;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.context.LivingWorldContextSnapshot;
import net.conczin.mca.util.WorldUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ChatAI {
    private static final int VILLAGER_SEARCH_RANGE = 32;
    private static final int CONVERSATION_TIME = 20 * 60;
    private static final int CONVERSATION_DISTANCE = 16;

    private static final Map<UUID, ChatAIStrategy> strategies = new ConcurrentHashMap<>();
    private static final Map<UUID, OpenConversation> currentConversations = new ConcurrentHashMap<>();

    public static Optional<String> answer(ServerPlayer player, VillagerEntityMCA villager, String msg) {
        ChatAIStrategy strategy = computeStrategyIfAbsent(villager.getUUID());
        openConversation(player, villager);
        return strategy.answer(player, villager, msg);
    }

    /** Snapshot-aware answer path for LivingWorld. Snapshot capture happens on the Minecraft server thread. */
    public static Optional<String> answer(
            MinecraftServer server,
            ServerPlayer player,
            VillagerEntityMCA villager,
            String msg,
            LivingWorldContextSnapshot snapshot
    ) {
        ChatAIStrategy strategy = computeStrategyIfAbsent(snapshot.villagerId());
        currentConversations.put(snapshot.playerId(), new OpenConversation(snapshot.villagerId(), snapshot.gameTime()));
        if (strategy instanceof OpenAIChatAI openAIChatAI) {
            return openAIChatAI.answer(server, player, villager, msg, snapshot);
        }
        return strategy.answer(player, villager, msg);
    }

    public static void openConversation(ServerPlayer player, VillagerEntityMCA villager) {
        long time = villager.level().getGameTime();
        currentConversations.put(player.getUUID(), new OpenConversation(villager.getUUID(), time));
    }

    public static boolean hasOpenConversation(UUID playerID) {
        return currentConversations.containsKey(playerID);
    }

    public static Optional<VillagerEntityMCA> getActiveConversationVillager(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        OpenConversation conv = currentConversations.getOrDefault(playerUUID, new OpenConversation(playerUUID, 0L));
        List<VillagerEntityMCA> nearbyVillagers = WorldUtils.getCloseEntities(player.level(), player, VILLAGER_SEARCH_RANGE, VillagerEntityMCA.class);
        Optional<VillagerEntityMCA> optionalVillager = nearbyVillagers.stream().filter(v -> conv.villagerUUID.equals(v.getUUID())).findFirst();
        if (optionalVillager.isPresent() && isInConversationWith(player, optionalVillager.get())) {
            return optionalVillager;
        }
        currentConversations.remove(playerUUID, conv);
        return Optional.empty();
    }

    private static ChatAIStrategy computeStrategyIfAbsent(UUID villagerID) {
        return strategies.computeIfAbsent(villagerID, v -> {
            String inworldResourceName = Config.getInstance().inworldAIResourceNames.getOrDefault(v, "");
            return inworldResourceName.isEmpty() ? new OpenAIChatAI() : new InworldAI(inworldResourceName);
        });
    }

    public static void clearStrategy(UUID villagerID) {
        strategies.remove(villagerID);
    }

    private static String getName(VillagerEntityMCA villager) {
        return normalizeString(villager.getName().getString());
    }

    public static Optional<VillagerEntityMCA> getVillagerForConversation(ServerPlayer player, String msg) {
        List<VillagerEntityMCA> nearbyVillagers = WorldUtils.getCloseEntities(player.level(), player, VILLAGER_SEARCH_RANGE, VillagerEntityMCA.class);
        String normalizedMsg = normalizeString(msg);
        for (VillagerEntityMCA villager : nearbyVillagers) {
            String normalizedName = getName(villager);
            String[] nameParts = normalizedName.split(" ");
            for (String part : nameParts) {
                if (Pattern.compile("\\b" + Pattern.quote(part) + "\\b").matcher(normalizedMsg).find()) {
                    return Optional.of(villager);
                }
            }
        }
        return getActiveConversationVillager(player);
    }

    private static boolean isInConversationWith(ServerPlayer player, VillagerEntityMCA villager) {
        OpenConversation conversation = currentConversations.getOrDefault(player.getUUID(), new OpenConversation(villager.getUUID(), 0L));
        return villager.distanceTo(player) < CONVERSATION_DISTANCE
               && villager.level().getGameTime() < conversation.lastInteractionTime + CONVERSATION_TIME;
    }

    public static Optional<VillagerEntityMCA> findVillagerInArea(ServerPlayer player, String searchName) {
        List<VillagerEntityMCA> entities = WorldUtils.getCloseEntities(player.level(), player, VILLAGER_SEARCH_RANGE, VillagerEntityMCA.class);
        String normalizedSearchName = normalizeString(searchName);
        for (VillagerEntityMCA villager : entities) {
            String villagerName = getName(villager);
            if (normalizedSearchName.equals(villagerName)) return Optional.of(villager);
        }
        return Optional.empty();
    }

    private static String normalizeString(String string) {
        return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private record OpenConversation(UUID villagerUUID, Long lastInteractionTime) {
    }
}
