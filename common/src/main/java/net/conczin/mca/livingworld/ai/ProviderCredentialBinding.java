package net.conczin.mca.livingworld.ai;

import java.util.Objects;

/** Selects credentials only after a provider destination has been validated and classified. */
public final class ProviderCredentialBinding {
    private ProviderCredentialBinding() {
    }

    public static String resolveChatKey(
            ProviderEndpoint endpoint,
            String configuredKey,
            String openAiEnvironmentKey,
            String openRouterEnvironmentKey
    ) {
        Objects.requireNonNull(endpoint, "endpoint");
        return switch (endpoint.family()) {
            case OPENAI -> firstNonBlank(openAiEnvironmentKey, configuredKey);
            case OPENROUTER -> firstNonBlank(openRouterEnvironmentKey, configuredKey);
            case CONCZIN, CUSTOM -> normalize(configuredKey);
        };
    }

    public static String resolveAudioKey(
            ProviderEndpoint audioEndpoint,
            String dedicatedKey,
            ProviderEndpoint chatEndpoint,
            String mainChatKey,
            String openAiEnvironmentKey,
            String openRouterEnvironmentKey
    ) {
        Objects.requireNonNull(audioEndpoint, "audioEndpoint");
        Objects.requireNonNull(chatEndpoint, "chatEndpoint");

        return switch (audioEndpoint.family()) {
            case OPENAI -> firstNonBlank(
                    openAiEnvironmentKey,
                    dedicatedKey,
                    chatEndpoint.family() == ProviderEndpoint.Family.OPENAI ? mainChatKey : ""
            );
            case OPENROUTER -> firstNonBlank(
                    openRouterEnvironmentKey,
                    dedicatedKey,
                    chatEndpoint.family() == ProviderEndpoint.Family.OPENROUTER ? mainChatKey : ""
            );
            case CONCZIN, CUSTOM -> normalize(dedicatedKey);
        };
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) return normalized;
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record BoundEndpoint(ProviderEndpoint endpoint, String apiKey) {
        public BoundEndpoint {
            endpoint = Objects.requireNonNull(endpoint, "endpoint");
            apiKey = normalize(apiKey);
        }
    }
}
