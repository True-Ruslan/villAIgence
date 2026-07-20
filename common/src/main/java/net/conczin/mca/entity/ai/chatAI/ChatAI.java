package net.conczin.mca.entity.ai.chatAI;

import net.conczin.mca.Config;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.util.WorldUtils;
import net.minecraft.server.level.ServerPlayer;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ChatAI {
    private static final int VILLAGER_SEARCH_RANGE = 32;
    private static final int CONVERSATION_TIME = 20 * 60;
    private static final int CONVERSATION_DISTANCE = 16;

    private static final Map<UUID, ChatAIStrategy> strategies = new HashMap<>();
    private static final Map<UUID, OpenConversation> currentConversations = new ConcurrentHashMap<>();

    public static Optional<String> answer(ServerPlayer player, VillagerEntityMCA villager, String msg) {
        ChatAIStrategy strategy = computeStrategyIfAbsent(villager.getUUID());
        openConversation(player, villager);
        return strategy.answer(player, villager, msg);
    }

    /**
     * Explicitly selects the villager as the player's current AI conversation target.
     * Voice input uses this method so ambient microphone audio is never routed to arbitrary NPCs.
     */
    public static void openConversation(ServerPlayer player, VillagerEntityMCA villager) {
        currentConversations.put(
                player.getUUID(),
                new OpenConversation(villager.getUUID(), villager.level().getGameTime())
        );
    }

    /**
     * Returns the player's still-valid explicit conversation target, if nearby and not expired.
     */
    public static Optional<VillagerEntityMCA> getActiveConversationVillager(ServerPlayer player) {
        OpenConversation conversation = currentConversations.get(player.getUUID());
        if (conversation == null) return Optional.empty();

        List<VillagerEntityMCA> nearbyVillagers = WorldUtils.getCloseEntities(
                player.level(), player, VILLAGER_SEARCH_RANGE, VillagerEntityMCA.class
        );
        Optional<VillagerEntityMCA> target = nearbyVillagers.stream()
                .filter(villager -> conversation.villagerUUID.equals(villager.getUUID()))
                .findFirst();

        if (target.isPresent() && isInConversationWith(player, target.get())) {
            return target;
        }
        currentConversations.remove(player.getUUID(), conversation);
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

    /**
     * Checks if the message contains the name of any specific nearby villager. If not, falls back
     * to the player's explicit active conversation target.
     */
    public static Optional<VillagerEntityMCA> getVillagerForConversation(ServerPlayer player, String msg) {
        List<VillagerEntityMCA> nearbyVillagers = WorldUtils.getCloseEntities(
                player.level(), player, VILLAGER_SEARCH_RANGE, VillagerEntityMCA.class
        );
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
        OpenConversation conversation = currentConversations.getOrDefault(
                player.getUUID(), new OpenConversation(villager.getUUID(), 0L)
        );
        return villager.distanceTo(player) < CONVERSATION_DISTANCE
                && villager.level().getGameTime() < conversation.lastInteractionTime + CONVERSATION_TIME;
    }

    public static Optional<VillagerEntityMCA> findVillagerInArea(ServerPlayer player, String searchName) {
        List<VillagerEntityMCA> entities = WorldUtils.getCloseEntities(
                player.level(), player, VILLAGER_SEARCH_RANGE, VillagerEntityMCA.class
        );
        String normalizedSearchName = normalizeString(searchName);
        for (VillagerEntityMCA villager : entities) {
            String villagerName = getName(villager);
            if (normalizedSearchName.equals(villagerName)) {
                return Optional.of(villager);
            }
        }
        return Optional.empty();
    }

    private static String normalizeString(String string) {
        return Normalizer.normalize(string, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private record OpenConversation(UUID villagerUUID, Long lastInteractionTime) {
    }
}
