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
        boolean chatCredential = hasValue(config.resolvedApiKey());
        AiStageConfig chat = new AiStageConfig(
                !config.enabled
                        ? AiConfigState.DISABLED
                        : config.isConfigured() ? AiConfigState.CONFIGURED : AiConfigState.MISCONFIGURED,
                config.enabled,
                chatCredential,
                provider,
                config.model,
                endpointHost(config.endpoint),
                ""
        );

        boolean sttCredential = hasValue(config.resolvedSttApiKey());
        AiStageConfig stt = new AiStageConfig(
                !config.voiceInputEnabled
                        ? AiConfigState.DISABLED
                        : config.isVoiceInputConfigured() ? AiConfigState.CONFIGURED : AiConfigState.MISCONFIGURED,
                config.voiceInputEnabled,
                sttCredential,
                providerForEndpoint(config.sttEndpoint, provider),
                config.sttModel,
                endpointHost(config.sttEndpoint),
                SttRequestFormat.parse(config.sttRequestFormat).resolve(config.sttEndpoint).configValue()
        );

        boolean ttsCredential = hasValue(config.resolvedTtsApiKey());
        AiStageConfig tts = new AiStageConfig(
                !config.voiceOutputEnabled
                        ? AiConfigState.DISABLED
                        : config.isVoiceOutputConfigured() ? AiConfigState.CONFIGURED : AiConfigState.MISCONFIGURED,
                config.voiceOutputEnabled,
                ttsCredential,
                providerForEndpoint(config.ttsEndpoint, provider),
                config.ttsModel,
                endpointHost(config.ttsEndpoint),
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
