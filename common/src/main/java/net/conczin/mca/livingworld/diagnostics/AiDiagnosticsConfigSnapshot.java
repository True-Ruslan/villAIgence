package net.conczin.mca.livingworld.diagnostics;

import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.voice.SttRequestFormat;
import net.conczin.mca.livingworld.voice.TtsResponseFormat;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/** Immutable configuration snapshot that intentionally contains no credential values. */
public record AiDiagnosticsConfigSnapshot(
        AiStageConfig chat,
        AiStageConfig stt,
        AiStageConfig tts
) {
    public AiDiagnosticsConfigSnapshot {
        chat = Objects.requireNonNull(chat, "chat");
        stt = Objects.requireNonNull(stt, "stt");
        tts = Objects.requireNonNull(tts, "tts");
    }

    public static AiDiagnosticsConfigSnapshot from(LivingWorldConfig config) {
        Objects.requireNonNull(config, "config");

        String provider = normalizeProvider(config.provider);
        String chatHost = endpointHost(config.endpoint);
        boolean chatCredential = hasValue(config.resolvedApiKey());
        AiStageConfig chat = new AiStageConfig(
                configState(config.enabled, config.isConfigured(), chatHost),
                config.enabled,
                chatCredential,
                provider,
                config.model,
                chatHost,
                ""
        );

        String sttHost = endpointHost(config.sttEndpoint);
        boolean sttCredential = hasValue(config.resolvedSttApiKey());
        AiStageConfig stt = new AiStageConfig(
                configState(config.voiceInputEnabled, config.isVoiceInputConfigured(), sttHost),
                config.voiceInputEnabled,
                sttCredential,
                providerForEndpoint(config.sttEndpoint, provider),
                config.sttModel,
                sttHost,
                SttRequestFormat.parse(config.sttRequestFormat).resolve(config.sttEndpoint).configValue()
        );

        String ttsHost = endpointHost(config.ttsEndpoint);
        boolean ttsCredential = hasValue(config.resolvedTtsApiKey());
        AiStageConfig tts = new AiStageConfig(
                configState(config.voiceOutputEnabled, config.isVoiceOutputConfigured(), ttsHost),
                config.voiceOutputEnabled,
                ttsCredential,
                providerForEndpoint(config.ttsEndpoint, provider),
                config.ttsModel,
                ttsHost,
                TtsResponseFormat.parse(config.ttsResponseFormat).resolve(config.ttsEndpoint).configValue()
        );

        return new AiDiagnosticsConfigSnapshot(chat, stt, tts);
    }

    static String endpointHost(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return "<invalid>";
        try {
            String host = URI.create(endpoint.trim()).getHost();
            return host == null || host.isBlank() ? "<invalid>" : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ignored) {
            return "<invalid>";
        }
    }

    private static AiConfigState configState(boolean enabled, boolean configured, String endpointHost) {
        if (!enabled) return AiConfigState.DISABLED;
        return configured && !"<invalid>".equals(endpointHost)
                ? AiConfigState.CONFIGURED
                : AiConfigState.MISCONFIGURED;
    }

    private static String providerForEndpoint(String endpoint, String fallback) {
        if (SttRequestFormat.isOpenRouterEndpoint(endpoint) || TtsResponseFormat.isOpenRouterEndpoint(endpoint)) {
            return "openrouter";
        }
        String host = endpointHost(endpoint);
        if ("api.openai.com".equals(host) || host.endsWith(".api.openai.com")) return "openai";
        return fallback;
    }

    private static String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
