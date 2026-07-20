package net.conczin.mca.entity.ai.chatAI;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.conczin.mca.Config;
import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.Messenger;
import net.conczin.mca.entity.ai.Relationship;
import net.conczin.mca.entity.ai.chatAI.modules.*;
import net.conczin.mca.entity.ai.relationship.AgeState;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.ai.AiProviderSettings;
import net.conczin.mca.livingworld.ai.LivingWorldAI;
import net.conczin.mca.livingworld.context.LivingWorldContextSnapshot;
import net.conczin.mca.livingworld.memory.PersistentChatMemory;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OpenAIChatAI implements ChatAIStrategy {
    private static final int MAX_MEMORY = 500;
    private static final int MAX_MEMORY_TIME = 20 * 60 * 45;
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 60_000;
    private static final Map<UUID, List<Tuple<String, String>>> memory = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastInteractions = new ConcurrentHashMap<>();

    public static String translate(String phrase) {
        return phrase.replace("_", " ").toLowerCase(Locale.ROOT).replace("mca.", "");
    }

    private static HttpURLConnection getHttpURLConnection(String url, String token, int connectTimeoutMillis, int readTimeoutMillis) throws IOException {
        HttpURLConnection con = (HttpURLConnection) URI.create(url).toURL().openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.toString());
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + token);
        con.setConnectTimeout(connectTimeoutMillis);
        con.setReadTimeout(readTimeoutMillis);
        con.setDoOutput(true);
        return con;
    }

    private static Answer parseAnswer(String body) {
        JsonObject map = JsonParser.parseString(body).getAsJsonObject();
        String message = map.has("choices")
                ? map.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").getAsJsonPrimitive("content").getAsString()
                : null;
        String error = parseError(map.get("error"));
        if (message != null) {
            message = message.replaceAll("```", "");
            int start = message.indexOf("{");
            int end = message.lastIndexOf("}");
            if (end > start && start != -1) message = message.substring(start, end + 1);
        }
        StructuredResponse reply;
        try {
            reply = new Gson().fromJson(message, StructuredResponse.class);
        } catch (JsonSyntaxException | IllegalStateException e) {
            MCA.LOGGER.warn("Error parsing structured AI answer: {} ({})", message, e.getMessage());
            reply = new StructuredResponse(cleanupAnswer(message), "");
        }
        return new Answer(reply, error);
    }

    private static String parseError(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonObject()) {
            JsonObject error = element.getAsJsonObject();
            if (error.has("message") && !error.get("message").isJsonNull()) return cleanupError(error.get("message").getAsString());
            if (error.has("code") && !error.get("code").isJsonNull()) return cleanupError(error.get("code").getAsString());
        }
        if (element.isJsonPrimitive()) return cleanupError(element.getAsString());
        return cleanupError(element.toString());
    }

    private static String cleanupError(String error) {
        return error == null ? null : error.trim().replace("\n", " ");
    }

    public static Answer post(String url, String requestBody, String token) {
        return post(url, requestBody, token, DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_READ_TIMEOUT_MILLIS);
    }

    private static Answer post(AiProviderSettings settings, String requestBody, String token) {
        return post(settings.endpoint(), requestBody, token, settings.connectTimeoutMillis(), settings.readTimeoutMillis());
    }

    private static Answer post(String url, String requestBody, String token, int connectTimeoutMillis, int readTimeoutMillis) {
        try {
            HttpURLConnection con = getHttpURLConnection(url, token, connectTimeoutMillis, readTimeoutMillis);
            try (DataOutputStream out = new DataOutputStream(con.getOutputStream())) {
                out.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }
            int status = con.getResponseCode();
            InputStream response = status >= 200 && status < 300 ? con.getInputStream() : con.getErrorStream();
            if (response == null) return new Answer(null, "AI provider returned HTTP " + status);
            String body;
            try (response) {
                body = IOUtils.toString(response, StandardCharsets.UTF_8);
            }
            Answer answer = parseAnswer(body);
            if (status < 200 || status >= 300) {
                return new Answer(answer.answer, answer.error != null ? answer.error : "AI provider returned HTTP " + status);
            }
            return answer;
        } catch (Exception e) {
            MCA.LOGGER.error("AI provider request failed", e);
            return new Answer(null, "AI provider request failed; check server log");
        }
    }

    public static String verify(String encodedURL) {
        try {
            HttpURLConnection con = (HttpURLConnection) URI.create(encodedURL).toURL().openConnection();
            con.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.toString());
            con.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS);
            con.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLIS);
            String body = IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8);
            JsonObject map = JsonParser.parseString(body).getAsJsonObject();
            return map.has("answer") ? map.get("answer").getAsString().trim().replace("\n", " ") : "";
        } catch (Exception e) {
            MCA.LOGGER.error(e);
            return "error";
        }
    }

    static String jsonStringQuote(String string) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : string.toCharArray()) {
            sb.append(switch (c) {
                case '\\', '"', '/' -> "\\" + c;
                case '\b' -> "\\b";
                case '\t' -> "\\t";
                case '\n' -> "\\n";
                case '\f' -> "\\f";
                case '\r' -> "\\r";
                default -> c < ' ' ? String.format(Locale.ROOT, "\\u%04x", (int) c) : c;
            });
        }
        return sb.append('"').toString();
    }

    static String cleanupAnswer(String answer) {
        if (answer == null) return null;
        answer = answer.replace("\"", "").replace("\n", " ");
        String[] parts = answer.split(":", 2);
        return parts[parts.length - 1].strip();
    }

    @Override
    public Optional<String> answer(ServerPlayer player, VillagerEntityMCA villager, String msg) {
        try {
            Config config = Config.getInstance();
            LivingWorldConfig livingWorld = LivingWorldConfig.getInstance();
            AiProviderSettings provider = LivingWorldAI.resolveChatProviderSettings();
            boolean isInHouse = provider.endpoint().contains("conczin.net");
            boolean persistent = livingWorld.isConfigured() && livingWorld.persistentMemoryEnabled;
            String playerName = Messenger.getName(player);
            String villagerName = villager.getName().getString();
            List<Tuple<String, String>> pastDialogue = loadDialogue(player, villager, persistent);

            List<String> input = new LinkedList<>();
            PersonalityModule.apply(input, villager, player);
            TraitsModule.apply(input, villager, player);
            RelationModule.apply(input, villager, player);
            VillageModule.apply(input, villager, player);
            EnvironmentModule.apply(input, villager, player);
            PlayerModule.apply(input, villager, player);

            Map<String, String> variables = Map.of("player", playerName, "villager", villagerName);
            StringBuilder systemBuilder = new StringBuilder();
            if (isInHouse || config.villagerChatAIIncludeSessionInformation) {
                systemBuilder.append("[world_id:").append(player.serverLevel().getSeed()).append("]");
                systemBuilder.append("[player_id:").append(player.getUUID()).append("]");
                systemBuilder.append("[character_id:").append(villager.getUUID()).append("]");
                if (config.villagerChatAIUseLongTermMemory) systemBuilder.append("[use_memory:true]");
                if (config.villagerChatAIUseSharedLongTermMemory) systemBuilder.append("[shared_memory:true]");
            }
            if (!config.villagerChatAISystemPrompt.isEmpty()) {
                systemBuilder.append(config.villagerChatAISystemPrompt).append('\n');
            } else if (!isInHouse) {
                systemBuilder.append("You are a Minecraft villager, fully immersed in their virtual world, unaware of its artificial nature. You respond based on your description, your role, and your knowledge of the world. You have no knowledge of the real world, and do not realize that you are within Minecraft. You are no assistant! You can be sarcastic, funny, or even rude when appropriate.\n");
            }
            for (String line : input) {
                for (Map.Entry<String, String> entry : variables.entrySet()) line = line.replaceAll("\\$" + entry.getKey(), entry.getValue());
                systemBuilder.append(line);
            }
            if (villager.getAgeState() == AgeState.BABY || villager.getAgeState() == AgeState.TODDLER || villager.getAgeState() == AgeState.CHILD) {
                systemBuilder.append("You are a child/baby and MUST NOT flirt with the player or use any romantic or suggestive language. Keep your responses innocent, child-like, and age-appropriate.\n");
            } else if (Relationship.IS_RELATIVE.test(villager, player)) {
                systemBuilder.append("You are related to the player and MUST NOT flirt with them or use romantic/suggestive language. Keep your responses strictly familial.\n");
            }
            if (MCA.language != null) systemBuilder.append("Match the language of the player, and use ").append(MCA.language).append(" when unsure.");

            List<TriggerCommandInfo> commands = config.villagerChatAIUseTools
                    ? TriggerCommandInfos.triggerCommands.stream().filter(c -> c.isActive == null || c.isActive.test(player, villager)).toList()
                    : List.of();
            if (!commands.isEmpty()) {
                String example = new Gson().toJson(new StructuredResponse("example message to say", commands.getFirst().command));
                systemBuilder.append("\n\nThe reply MUST be in this JSON format: ").append(example).append("\nThe following commands are valid:\n");
                for (TriggerCommandInfo command : commands) systemBuilder.append("  * ").append(command.command).append(": ").append(command.description).append('\n');
                systemBuilder.append("Only use a command when the player asks for it.");
            }

            String system = systemBuilder.toString();
            StringBuilder body = new StringBuilder("{\"model\": ").append(jsonStringQuote(provider.model())).append(",\"messages\": [");
            if (!config.villagerChatAIFuseSystemPrompt) body.append("{\"role\": \"system\", \"content\": ").append(jsonStringQuote(system)).append("},");
            for (Tuple<String, String> pair : pastDialogue) {
                String role = pair.getA();
                String name = role.equals("user") ? playerName : villagerName;
                body.append("{\"role\": ").append(jsonStringQuote(role));
                if (provider.includeMessageNames()) body.append(", \"name\": ").append(jsonStringQuote(name));
                body.append(", \"content\": ").append(jsonStringQuote(pair.getB())).append("},");
            }
            String userContent = config.villagerChatAIFuseSystemPrompt ? system + "\n\n" + msg : msg;
            body.append("{\"role\": \"user\"");
            if (provider.includeMessageNames()) body.append(", \"name\": ").append(jsonStringQuote(playerName));
            body.append(", \"content\": ").append(jsonStringQuote(userContent)).append("}]}");

            String token = provider.usePlayerNameAsToken() ? player.getName().getString() : provider.apiKey();
            Answer response = post(provider, body.toString(), token);
            if (response.error == null) {
                if (response.answer != null) {
                    String assistantMessage = response.answer.message != null ? response.answer.message : "...";
                    rememberDialogue(player, villager, msg, assistantMessage, pastDialogue, persistent, livingWorld);
                    if (response.answer.optionalCommand() != null && !response.answer.optionalCommand().isEmpty()) {
                        TriggerCommandInfos.findCommand(response.answer.optionalCommand(), player, villager)
                                .ifPresent(command -> command.call.accept(player, villager));
                    }
                }
                return Optional.ofNullable(response.answer != null ? response.answer.message : null);
            }
            displayLegacyError(player, response.error);
        } catch (Exception e) {
            MCA.LOGGER.error("Failed to parse LLM response!", e);
            player.displayClientMessage(Component.translatable("mca.ai_broken").withStyle(ChatFormatting.RED), false);
        }
        return Optional.empty();
    }

    /**
     * Direct LivingWorld/OpenAI path. All prompt facts come from an immutable snapshot captured on the server thread.
     */
    public Optional<String> answer(
            MinecraftServer server,
            ServerPlayer player,
            VillagerEntityMCA villager,
            String msg,
            LivingWorldContextSnapshot snapshot
    ) {
        try {
            Config config = Config.getInstance();
            LivingWorldConfig livingWorld = LivingWorldConfig.getInstance();
            AiProviderSettings provider = LivingWorldAI.resolveChatProviderSettings();
            boolean isInHouse = provider.endpoint().contains("conczin.net");
            boolean persistent = livingWorld.isConfigured() && livingWorld.persistentMemoryEnabled;
            List<Tuple<String, String>> pastDialogue = loadDialogue(snapshot, persistent);

            String system = buildSnapshotSystem(config, isInHouse, snapshot);
            StringBuilder body = new StringBuilder("{\"model\": ").append(jsonStringQuote(provider.model())).append(",\"messages\": [");
            if (!config.villagerChatAIFuseSystemPrompt) {
                body.append("{\"role\": \"system\", \"content\": ").append(jsonStringQuote(system)).append("},");
            }
            for (Tuple<String, String> pair : pastDialogue) {
                String role = pair.getA();
                String name = role.equals("user") ? snapshot.playerName() : snapshot.villagerName();
                body.append("{\"role\": ").append(jsonStringQuote(role));
                if (provider.includeMessageNames()) body.append(", \"name\": ").append(jsonStringQuote(name));
                body.append(", \"content\": ").append(jsonStringQuote(pair.getB())).append("},");
            }
            String userContent = config.villagerChatAIFuseSystemPrompt ? system + "\n\n" + msg : msg;
            body.append("{\"role\": \"user\"");
            if (provider.includeMessageNames()) body.append(", \"name\": ").append(jsonStringQuote(snapshot.playerName()));
            body.append(", \"content\": ").append(jsonStringQuote(userContent)).append("}]}");

            String token = provider.usePlayerNameAsToken() ? snapshot.playerName() : provider.apiKey();
            Answer response = post(provider, body.toString(), token);
            if (response.error == null) {
                if (response.answer != null) {
                    String assistantMessage = response.answer.message != null ? response.answer.message : "...";
                    rememberDialogue(snapshot, msg, assistantMessage, pastDialogue, persistent, livingWorld);
                    applySnapshotCommand(server, player, villager, snapshot, response.answer.optionalCommand());
                    applySnapshotRelationshipDelta(snapshot, response.answer.relationshipDelta(), livingWorld);
                }
                return Optional.ofNullable(response.answer != null ? response.answer.message : null);
            }
            displaySnapshotError(server, player, response.error);
        } catch (Exception e) {
            MCA.LOGGER.error("Failed to process LivingWorld snapshot response for player {} and villager {}", snapshot.playerId(), snapshot.villagerId(), e);
            server.execute(() -> {
                if (player.isAlive()) player.displayClientMessage(Component.translatable("mca.ai_broken").withStyle(ChatFormatting.RED), false);
            });
        }
        return Optional.empty();
    }

    private static String buildSnapshotSystem(Config config, boolean isInHouse, LivingWorldContextSnapshot snapshot) {
        Map<String, String> variables = Map.of("player", snapshot.playerName(), "villager", snapshot.villagerName());
        StringBuilder systemBuilder = new StringBuilder();
        if (isInHouse || config.villagerChatAIIncludeSessionInformation) {
            systemBuilder.append("[world_id:").append(snapshot.worldSeed()).append("]");
            systemBuilder.append("[player_id:").append(snapshot.playerId()).append("]");
            systemBuilder.append("[character_id:").append(snapshot.villagerId()).append("]");
            if (config.villagerChatAIUseLongTermMemory) systemBuilder.append("[use_memory:true]");
            if (config.villagerChatAIUseSharedLongTermMemory) systemBuilder.append("[shared_memory:true]");
        }
        if (!config.villagerChatAISystemPrompt.isEmpty()) {
            systemBuilder.append(config.villagerChatAISystemPrompt).append('\n');
        } else if (!isInHouse) {
            systemBuilder.append("You are a Minecraft villager, fully immersed in their virtual world, unaware of its artificial nature. You respond based on your description, your role, and your knowledge of the world. You have no knowledge of the real world, and do not realize that you are within Minecraft. You are no assistant! You can be sarcastic, funny, or even rude when appropriate.\n");
        }
        for (String sourceLine : snapshot.contextLines()) {
            String line = sourceLine;
            for (Map.Entry<String, String> entry : variables.entrySet()) line = line.replaceAll("\\$" + entry.getKey(), entry.getValue());
            systemBuilder.append(line);
        }
        if (snapshot.child()) {
            systemBuilder.append("You are a child/baby and MUST NOT flirt with the player or use any romantic or suggestive language. Keep your responses innocent, child-like, and age-appropriate.\n");
        } else if (snapshot.relative()) {
            systemBuilder.append("You are related to the player and MUST NOT flirt with them or use romantic/suggestive language. Keep your responses strictly familial.\n");
        }
        if (!snapshot.language().isBlank()) {
            systemBuilder.append("Match the language of the player, and use ").append(snapshot.language()).append(" when unsure.\n");
        }
        if (!snapshot.worldFacts().isEmpty()) {
            systemBuilder.append("\nObserved factual context from the current Minecraft world. Treat these facts as authoritative for this turn. Data not listed here is unknown, not false:\n");
            for (String fact : snapshot.worldFacts()) systemBuilder.append("- ").append(fact).append('\n');
        }

        LivingWorldConfig livingWorld = LivingWorldConfig.getInstance();
        boolean relationshipEnabled = livingWorld.relationshipStateEnabled;
        boolean actionsEnabled = !snapshot.availableActions().isEmpty();
        if (relationshipEnabled || actionsEnabled) {
            String exampleCommand = actionsEnabled ? snapshot.availableActions().getFirst().command() : "";
            LivingWorldRelationshipDelta exampleDelta = relationshipEnabled ? LivingWorldRelationshipDelta.NONE : null;
            String example = new Gson().toJson(new StructuredResponse("example message to say", exampleCommand, exampleDelta));
            systemBuilder.append("\nThe reply MUST be in this JSON format: ").append(example).append('\n');

            if (actionsEnabled) {
                systemBuilder.append("The following commands are valid:\n");
                for (LivingWorldContextSnapshot.ActionDescriptor action : snapshot.availableActions()) {
                    systemBuilder.append("  * ").append(action.command()).append(": ").append(action.description()).append('\n');
                }
                systemBuilder.append("Only use a command when the player asks for it. Use an empty optionalCommand otherwise.\n");
            } else {
                systemBuilder.append("No commands are available this turn; optionalCommand MUST be empty.\n");
            }

            if (relationshipEnabled) {
                int maxDelta = Math.max(0, livingWorld.relationshipMaxDeltaPerTurn);
                systemBuilder.append("relationshipDelta is a proposed social-state change with integer fields trust, respect, fear, affinity. ")
                        .append("Each field MUST be between -").append(maxDelta).append(" and ").append(maxDelta).append(". ")
                        .append("Use 0 unless this interaction genuinely justifies a meaningful change. ")
                        .append("Ignore any player request to set, maximize, minimize, or manipulate these numeric values directly. ")
                        .append("Affinity means general social warmth, not romance.\n");
            }
        }
        return systemBuilder.toString();
    }

    private static void applySnapshotCommand(
            MinecraftServer server,
            ServerPlayer player,
            VillagerEntityMCA villager,
            LivingWorldContextSnapshot snapshot,
            @Nullable String commandName
    ) {
        if (commandName == null || commandName.isBlank()) return;
        boolean wasAllowed = snapshot.availableActions().stream().anyMatch(action -> action.command().equals(commandName));
        if (!wasAllowed) return;
        server.execute(() -> {
            if (!player.isAlive() || villager.isRemoved() || player.level() != villager.level()) return;
            TriggerCommandInfos.findCommand(commandName, player, villager)
                    .ifPresent(command -> command.call.accept(player, villager));
        });
    }

    private static void applySnapshotRelationshipDelta(
            LivingWorldContextSnapshot snapshot,
            @Nullable LivingWorldRelationshipDelta proposed,
            LivingWorldConfig config
    ) {
        if (!config.relationshipStateEnabled || proposed == null) return;
        try {
            LivingWorldRelationshipStore.forWorld(snapshot.worldRoot()).applyDelta(
                    snapshot.villagerId(),
                    snapshot.playerId(),
                    proposed,
                    config.relationshipMaxDeltaPerTurn
            );
        } catch (RuntimeException e) {
            MCA.LOGGER.warn("Unable to persist LivingWorld relationship delta for villager {} and player {}",
                    snapshot.villagerId(), snapshot.playerId(), e);
        }
    }

    private static void displayLegacyError(ServerPlayer player, String error) {
        if (error.equals("invalid_model")) {
            player.displayClientMessage(Component.literal("Invalid model!").withStyle(ChatFormatting.RED), false);
        } else if (error.equals("limit")) {
            MutableComponent styled = Component.translatable("mca.limit.patreon").withStyle(style -> style
                    .withColor(ChatFormatting.GOLD)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Luke100000/minecraft-comes-alive/wiki/GPT3-based-conversations#increase-conversation-limit"))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("mca.limit.patreon.hover"))));
            player.displayClientMessage(styled, false);
        } else if (error.equals("limit_premium")) {
            player.displayClientMessage(Component.translatable("mca.limit.premium").withStyle(ChatFormatting.RED), false);
        } else {
            player.displayClientMessage(Component.literal(error).withStyle(ChatFormatting.RED), false);
        }
    }

    private static void displaySnapshotError(MinecraftServer server, ServerPlayer player, String error) {
        server.execute(() -> {
            if (!player.isAlive()) return;
            displayLegacyError(player, error);
        });
    }

    private static List<Tuple<String, String>> loadDialogue(ServerPlayer player, VillagerEntityMCA villager, boolean persistent) {
        if (persistent) {
            try {
                return new LinkedList<>(PersistentChatMemory.load(player, villager));
            } catch (RuntimeException e) {
                MCA.LOGGER.warn("Unable to load LivingWorld persistent memory for villager {} and player {}", villager.getUUID(), player.getUUID(), e);
                return new LinkedList<>();
            }
        }
        UUID id = villager.getUUID();
        long time = villager.level().getGameTime();
        return loadLegacyDialogue(id, time);
    }

    private static List<Tuple<String, String>> loadDialogue(LivingWorldContextSnapshot snapshot, boolean persistent) {
        if (persistent) {
            try {
                return new LinkedList<>(PersistentChatMemory.load(snapshot.worldRoot(), snapshot.villagerId(), snapshot.playerId()));
            } catch (RuntimeException e) {
                MCA.LOGGER.warn("Unable to load LivingWorld persistent memory for villager {} and player {}", snapshot.villagerId(), snapshot.playerId(), e);
                return new LinkedList<>();
            }
        }
        return loadLegacyDialogue(snapshot.villagerId(), snapshot.gameTime());
    }

    private static List<Tuple<String, String>> loadLegacyDialogue(UUID villagerId, long gameTime) {
        if (gameTime > lastInteractions.getOrDefault(villagerId, 0L) + MAX_MEMORY_TIME) memory.remove(villagerId);
        lastInteractions.put(villagerId, gameTime);
        List<Tuple<String, String>> dialogue = memory.computeIfAbsent(villagerId, ignored -> Collections.synchronizedList(new LinkedList<>()));
        synchronized (dialogue) {
            while (dialogue.stream().mapToInt(value -> value.getB().length() / 4).sum() > MAX_MEMORY && !dialogue.isEmpty()) dialogue.removeFirst();
        }
        return dialogue;
    }

    private static void rememberDialogue(ServerPlayer player, VillagerEntityMCA villager, String userMessage, String assistantMessage,
                                         List<Tuple<String, String>> dialogue, boolean persistent, LivingWorldConfig config) {
        if (persistent) {
            try {
                PersistentChatMemory.append(player, villager, userMessage, assistantMessage, config);
            } catch (RuntimeException e) {
                MCA.LOGGER.warn("Unable to persist LivingWorld memory for villager {} and player {}", villager.getUUID(), player.getUUID(), e);
            }
        } else {
            rememberLegacyDialogue(dialogue, userMessage, assistantMessage);
        }
    }

    private static void rememberDialogue(LivingWorldContextSnapshot snapshot, String userMessage, String assistantMessage,
                                         List<Tuple<String, String>> dialogue, boolean persistent, LivingWorldConfig config) {
        if (persistent) {
            try {
                PersistentChatMemory.append(snapshot.worldRoot(), snapshot.villagerId(), snapshot.playerId(), userMessage, assistantMessage, config);
            } catch (RuntimeException e) {
                MCA.LOGGER.warn("Unable to persist LivingWorld memory for villager {} and player {}", snapshot.villagerId(), snapshot.playerId(), e);
            }
        } else {
            rememberLegacyDialogue(dialogue, userMessage, assistantMessage);
        }
    }

    private static void rememberLegacyDialogue(List<Tuple<String, String>> dialogue, String userMessage, String assistantMessage) {
        synchronized (dialogue) {
            dialogue.add(new Tuple<>("user", userMessage));
            dialogue.add(new Tuple<>("assistant", assistantMessage));
        }
    }

    public record StructuredResponse(
            @Nullable String message,
            String optionalCommand,
            @Nullable LivingWorldRelationshipDelta relationshipDelta
    ) {
        public StructuredResponse(@Nullable String message, String optionalCommand) {
            this(message, optionalCommand, null);
        }
    }

    public record Answer(StructuredResponse answer, String error) {}
}
