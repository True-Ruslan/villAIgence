package net.conczin.mca.livingworld.voice;

import net.conczin.mca.livingworld.ai.ProviderEndpoint;
import net.conczin.mca.livingworld.ai.ProviderEndpointPolicy;

import java.util.Locale;

/** Transport format used by speech-to-text providers. */
public enum SttRequestFormat {
    AUTO("auto"),
    MULTIPART("multipart"),
    JSON_BASE64("json_base64");

    private final String configValue;

    SttRequestFormat(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static SttRequestFormat parse(String value) {
        if (value == null || value.isBlank()) return AUTO;
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (SttRequestFormat format : values()) {
            if (format.configValue.equals(normalized)) return format;
        }
        return AUTO;
    }

    public SttRequestFormat resolve(String endpoint) {
        if (this != AUTO) return this;
        return isOpenRouterEndpoint(endpoint) ? JSON_BASE64 : MULTIPART;
    }

    public SttRequestFormat resolve(ProviderEndpoint endpoint) {
        if (this != AUTO) return this;
        return endpoint.family() == ProviderEndpoint.Family.OPENROUTER ? JSON_BASE64 : MULTIPART;
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
