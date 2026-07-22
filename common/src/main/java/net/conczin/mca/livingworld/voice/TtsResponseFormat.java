package net.conczin.mca.livingworld.voice;

import java.net.URI;
import java.util.Locale;

/** Provider-neutral transport format used for text-to-speech responses. */
public enum TtsResponseFormat {
    AUTO("auto"),
    WAV("wav"),
    PCM("pcm");

    private final String configValue;

    TtsResponseFormat(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static TtsResponseFormat parse(String value) {
        if (value == null || value.isBlank()) return AUTO;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (TtsResponseFormat format : values()) {
            if (format.configValue.equals(normalized)) return format;
        }
        return AUTO;
    }

    public TtsResponseFormat resolve(String endpoint) {
        if (this != AUTO) return this;
        return isOpenRouterEndpoint(endpoint) ? PCM : WAV;
    }

    static boolean isOpenRouterEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return false;
        try {
            String host = URI.create(endpoint.trim()).getHost();
            if (host == null) return false;
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return normalizedHost.equals("openrouter.ai") || normalizedHost.endsWith(".openrouter.ai");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
