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
        if (livingWorld.isConfigured()) {
            ProviderCredentialBinding.BoundEndpoint binding = livingWorld.resolvedChatEndpoint();
            return new AiProviderSettings(
                    binding.endpoint(),
                    livingWorld.model,
                    binding.apiKey(),
                    AiProviderSettings.secondsToMillis(livingWorld.connectTimeoutSeconds),
                    AiProviderSettings.secondsToMillis(livingWorld.readTimeoutSeconds),
                    false,
                    false
            );
        }

        Config legacy = Config.getInstance();
        String legacyEndpointText = legacy.villagerChatAIEndpoint == null ? "" : legacy.villagerChatAIEndpoint;
        ProviderEndpoint legacyEndpoint = ProviderEndpointPolicy.parse(legacyEndpointText, false);
        String legacyToken = legacy.villagerChatAIToken == null ? "" : legacy.villagerChatAIToken;
        return new AiProviderSettings(
                legacyEndpoint,
                legacy.villagerChatAIModel == null ? "default" : legacy.villagerChatAIModel,
                legacyToken,
                AiProviderSettings.secondsToMillis(LEGACY_CONNECT_TIMEOUT_SECONDS),
                AiProviderSettings.secondsToMillis(LEGACY_READ_TIMEOUT_SECONDS),
                shouldUsePlayerNameAsToken(legacyEndpoint, legacyToken),
                true
        );
    }

    static boolean shouldUsePlayerNameAsToken(ProviderEndpoint endpoint, String legacyToken) {
        return legacyToken == null || legacyToken.isBlank() || endpoint.trustedConczin();
    }

    static AiProviderSettings selectSettings(
            boolean livingWorldConfigured,
            AiProviderSettings livingWorldSettings,
            AiProviderSettings legacySettings
    ) {
        return livingWorldConfigured ? livingWorldSettings : legacySettings;
    }
}
