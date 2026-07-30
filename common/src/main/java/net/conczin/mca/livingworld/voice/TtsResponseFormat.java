package net.conczin.mca.livingworld.voice;

import net.conczin.mca.livingworld.ai.ProviderEndpoint;
import net.conczin.mca.livingworld.ai.ProviderEndpointPolicy;

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

    public TtsResponseFormat resolve(ProviderEndpoint endpoint) {
        if (this != AUTO) return this;
        return endpoint.family() == ProviderEndpoint.Family.OPENROUTER ? PCM : WAV;
    }

    public static boolean isOpenRouterEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return false;
        try {
            return ProviderEndpointPolicy.parse(endpoint, true).family() == ProviderEndpoint.Family.OPENROUTER;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
