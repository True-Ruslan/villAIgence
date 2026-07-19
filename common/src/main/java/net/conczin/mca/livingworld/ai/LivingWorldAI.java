package net.conczin.mca.livingworld.ai;

import net.conczin.mca.Config;
import net.conczin.mca.livingworld.LivingWorldConfig;

/**
 * Single entry point for resolving villager chat AI configuration.
 */
public final class LivingWorldAI {
    private static final int LEGACY_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int LEGACY_READ_TIMEOUT_SECONDS = 60;

    private LivingWorldAI() {
    }

    public static boolean isChatEnabled() {
        return LivingWorldConfig.getInstance().isConfigured() || Config.getInstance().enableVillagerChatAI;
    }

    public static AiProviderSettings resolveChatProviderSettings() {
        LivingWorldConfig livingWorld = LivingWorldConfig.getInstance();
        Config legacy = Config.getInstance();

        AiProviderSettings livingWorldSettings = new AiProviderSettings(
                livingWorld.endpoint,
                livingWorld.model,
                livingWorld.resolvedApiKey(),
                AiProviderSettings.secondsToMillis(livingWorld.connectTimeoutSeconds),
                AiProviderSettings.secondsToMillis(livingWorld.readTimeoutSeconds),
                false
        );

        String legacyEndpoint = legacy.villagerChatAIEndpoint == null ? "" : legacy.villagerChatAIEndpoint;
        String legacyToken = legacy.villagerChatAIToken == null ? "" : legacy.villagerChatAIToken;
        boolean usePlayerNameAsToken = legacyToken.isBlank() || legacyEndpoint.contains("conczin.net");
        AiProviderSettings legacySettings = new AiProviderSettings(
                legacyEndpoint,
                legacy.villagerChatAIModel == null ? "default" : legacy.villagerChatAIModel,
                legacyToken,
                AiProviderSettings.secondsToMillis(LEGACY_CONNECT_TIMEOUT_SECONDS),
                AiProviderSettings.secondsToMillis(LEGACY_READ_TIMEOUT_SECONDS),
                usePlayerNameAsToken
        );

        return selectSettings(livingWorld.isConfigured(), livingWorldSettings, legacySettings);
    }

    static AiProviderSettings selectSettings(
            boolean livingWorldConfigured,
            AiProviderSettings livingWorldSettings,
            AiProviderSettings legacySettings
    ) {
        return livingWorldConfigured ? livingWorldSettings : legacySettings;
    }
}
