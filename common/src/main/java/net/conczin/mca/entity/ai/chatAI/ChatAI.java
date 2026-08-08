package net.conczin.mca.entity.ai.chatAI;

import net.conczin.mca.Config;
import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.ai.AiRequestDeadline;
import net.conczin.mca.livingworld.context.LivingWorldContextSnapshot;
import net.conczin.mca.livingworld.memory2.Memory2DialogueLifecycle;
import net.conczin.mca.livingworld.memory2.MemoryEvent;
import net.conczin.mca.livingworld.memory2.PlayerToldBeliefLifecycle;
import net.conczin.mca.util.WorldUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ChatAI {
    /**
     * Max range to find a villager in
     */
    private static final int VILLAGER_SEARCH_RANGE = 32;

    /**
     * Max time until a conversation is considered invalid
     */
    private static final int CONVERSATION_TIME = 20 * 60;

    /**
     * Max distance until a conversation is considered invalid
     */
    private static final int CONVERSATION_DISTANCE = 16;

    /**
     * Map of villager UUIDs to strategies (i.e. managed by InworldAI or GPT3)
     */
    private static final Map<UUID, ChatAIStrategy> strategies = new ConcurrentHashMap<>();

    /**
     * Current conversation of player. <p>
     * A player can max. have 1 conversation at all times.
     */
    private static final Map<UUID, OpenConversation> currentConversations = new ConcurrentHashMap<>();

    /**
     * Gets an answer for a specific message for a villager from a player with the villager-specific chat strategy
     *
     * @param player   ServerPlayerEntity of the player
     * @param villager VillagerEntityMCA of the villager
     * @param msg      Message in question
     * @return {@code Optional.EMPTY} if answer couldn't be generated, Optional containing answer String otherwise.
     */
    public static Optional<String> answer(ServerPlayer player, VillagerEntityMCA villager, String msg) {
        ChatAIStrategy strategy = computeStrategyIfAbsent(villager.getUUID());
        openConversation(player, villager);

        DialogueMemoryCoordinates memoryCoordinates = strategy instanceof OpenAIChatAI
                ? captureDialogueMemoryCoordinates(player, villager)
                : null;

        Optional<String> answer = strategy.answer(player, villager, msg);
        if (memoryCoordinates != null) {
            rememberMemory2Dialogue(memoryCoordinates, msg, answer);
        }
        return answer;
    }

    /**
     * Snapshot-aware answer path for LivingWorld. Snapshot capture happens on the Minecraft server thread.
     */
    public static Optional<String> answer(
            MinecraftServer server,
            ServerPlayer player,
            VillagerEntityMCA villager,
            String msg,
            LivingWorldContextSnapshot snapshot
    ) {
        return answer(server, player, villager, msg, snapshot, null);
    }

    public static Optional<String> answer(
            MinecraftServer server,
            ServerPlayer player,
            VillagerEntityMCA villager,
            String msg,
            LivingWorldContextSnapshot snapshot,
            @Nullable AiRequestDeadline deadline
    ) {
        ChatAIStrategy strategy = computeStrategyIfAbsent(snapshot.villagerId());
        currentConversations.put(snapshot.playerId(), new OpenConversation(snapshot.villagerId(), snapshot.gameTime()));
        if (strategy instanceof OpenAIChatAI openAIChatAI) {
            OpenAIChatAI.SnapshotAnswer snapshotAnswer =
                    openAIChatAI.answerDetailed(server, player, villager, msg, snapshot, deadline);
            Optional<String> answer = snapshotAnswer.message();
            Optional<MemoryEvent> sourceEvent = rememberMemory2Dialogue(
                    new DialogueMemoryCoordinates(
                            snapshot.worldRoot(),
                            snapshot.villagerId(),
                            snapshot.playerId(),
                            snapshot.gameTime()
                    ),
                    msg,
                    answer
            );
            LivingWorldConfig config = LivingWorldConfig.getInstance();
            sourceEvent.ifPresent(source -> {
                try {
                    PlayerToldBeliefLifecycle.recordCandidatesIfEnabled(
                            config.memory2Enabled && config.semanticBeliefExtractionEnabled,
                            snapshot.worldRoot(),
                            source,
                            snapshot.playerId(),
                            snapshotAnswer.beliefCandidates(),
                            config.semanticBeliefMaxCandidatesPerTurn,
                            config.memory2MaxEventsPerNpc
                    );
                } catch (RuntimeException e) {
                    MCA.LOGGER.warn(
                            "Unable to persist player-told semantic BELIEF candidates for villager {} and player {}",
                            snapshot.villagerId(),
                            snapshot.playerId(),
                            e
                    );
                }
            });
            return answer;
        }
        return strategy.answer(player, villager, msg);
    }

    private static DialogueMemoryCoordinates captureDialogueMemoryCoordinates(
            ServerPlayer player,
            VillagerEntityMCA villager
    ) {
        try {
            return new DialogueMemoryCoordinates(
                    player.serverLevel().getServer().getWorldPath(LevelResource.ROOT),
                    villager.getUUID(),
                    player.getUUID(),
                    villager.level().getGameTime()
            );
        } catch (RuntimeException e) {
            MCA.LOGGER.warn(
                    "Unable to capture Memory 2.0 dialogue coordinates for villager {} and player {}",
                    villager.getUUID(),
                    player.getUUID(),
                    e
            );
            return null;
        }
    }

    private static Optional<MemoryEvent> rememberMemory2Dialogue(
            DialogueMemoryCoordinates coordinates,
            String playerMessage,
            Optional<String> answer
    ) {
        if (coordinates == null) return Optional.empty();
        LivingWorldConfig config = LivingWorldConfig.getInstance();
        try {
            return Memory2DialogueLifecycle.recordSuccessful(
                    config.memory2Enabled,
                    coordinates.worldRoot(),
                    coordinates.villagerId(),
                    coordinates.playerId(),
                    coordinates.gameTime(),
                    playerMessage,
                    answer,
                    config.memory2MaxEventsPerNpc,
                    System.currentTimeMillis()
            );
        } catch (RuntimeException e) {
            MCA.LOGGER.warn(
                    "Unable to persist Memory 2.0 dialogue event for villager {} and player {}",
                    coordinates.villagerId(),
                    coordinates.playerId(),
                    e
            );
            return Optional.empty();
        }
    }

    /**
     * Explicitly selects a villager as the player's current AI conversation target.
     */
    public static void openConversation(ServerPlayer player, VillagerEntityMCA villager) {
        long time = villager.level().getGameTime();
        currentConversations.put(player.getUUID(), new OpenConversation(villager.getUUID(), time));
    }

    /**
     * Cheap gate for microphone packet handling. Full target validation happens on the server thread.
     */
    public static boolean hasOpenConversation(UUID playerID) {
        return currentConversations.containsKey(playerID);
    }

    /**
     * Returns the current conversation target if it is still nearby and within the existing MCA timeout.
     */
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

    /**
     * Searches Config for a map entry for UUID, uses Inworld with said entry if found, else GPT3 (default)
     *
     * @param villagerID UUID of villager
     * @return Object implementing the ChatAIStrategy interface
     */
    private static ChatAIStrategy computeStrategyIfAbsent(UUID villagerID) {
        return strategies.computeIfAbsent(villagerID, v -> {
            String inworldResourceName = Config.getInstance().inworldAIResourceNames.getOrDefault(v, "");
            return inworldResourceName.isEmpty() ? new DiagnosticsOpenAIChatAI() : new InworldAI(inworldResourceName);
        });
    }

    /**
     * Clears the strategy for a specific villager
     *
     * @param villagerID UUID of villager
     */
    public static void clearStrategy(UUID villagerID) {
        strategies.remove(villagerID);
    }

    private static String getName(VillagerEntityMCA villager) {
        return normalizeString(villager.getName().getString());
    }

    /**
     * Checks if the message contains the name of any specific villagers and that villager is nearby. First match.
     * If not, checks if the player has a valid active conversation with a nearby villager.
     */
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

    /**
     * Checks if a player is in a conversation with a villager
     */
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
            if (normalizedSearchName.equals(villagerName)) {
                return Optional.of(villager);
            }
        }
        return Optional.empty();
    }

    private static String normalizeString(String string) {
        return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private record DialogueMemoryCoordinates(Path worldRoot, UUID villagerId, UUID playerId, long gameTime) {
    }

    private record OpenConversation(UUID villagerUUID, Long lastInteractionTime) {
    }
}
