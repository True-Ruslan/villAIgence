package net.conczin.mca.livingworld.ai;

import java.util.Objects;

/**
 * Runtime settings required by an OpenAI-compatible chat provider.
 * Secrets are intentionally carried only in memory and must never be logged.
 */
public record AiProviderSettings(
        String endpoint,
        String model,
        String apiKey,
        int connectTimeoutMillis,
        int readTimeoutMillis,
        boolean usePlayerNameAsToken,
        boolean includeMessageNames
) {
    public AiProviderSettings {
        endpoint = Objects.requireNonNull(endpoint, "endpoint");
        model = Objects.requireNonNull(model, "model");
        apiKey = apiKey == null ? "" : apiKey;
        connectTimeoutMillis = Math.max(1_000, connectTimeoutMillis);
        readTimeoutMillis = Math.max(1_000, readTimeoutMillis);
    }

    static int secondsToMillis(int seconds) {
        long safeSeconds = Math.max(1L, seconds);
        return (int) Math.min(Integer.MAX_VALUE, safeSeconds * 1_000L);
    }
}
