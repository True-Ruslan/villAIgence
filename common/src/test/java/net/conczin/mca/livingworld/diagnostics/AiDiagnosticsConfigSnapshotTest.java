package net.conczin.mca.livingworld.diagnostics;

import net.conczin.mca.livingworld.LivingWorldConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiDiagnosticsConfigSnapshotTest {
    @Test
    void exposesOnlySafeConfigurationMetadata() {
        LivingWorldConfig config = new LivingWorldConfig();
        config.enabled = true;
        config.provider = "openrouter";
        config.apiKey = "SECRET_SENTINEL";
        config.endpoint = "https://user:ENDPOINT_SECRET@openrouter.ai/api/v1/chat/completions?token=QUERY_SECRET";
        config.model = "openai/gpt-4.1-mini";
        config.voiceInputEnabled = true;
        config.sttApiKey = "STT_SECRET_SENTINEL";
        config.sttEndpoint = "https://openrouter.ai/api/v1/audio/transcriptions";
        config.sttModel = "openai/gpt-4o-mini-transcribe";
        config.sttRequestFormat = "auto";
        config.voiceOutputEnabled = false;
        config.ttsApiKey = "TTS_SECRET_SENTINEL";
        config.ttsEndpoint = "https://openrouter.ai/api/v1/audio/speech";
        config.ttsModel = "openai/gpt-4o-mini-tts";
        config.ttsResponseFormat = "auto";

        AiDiagnosticsConfigSnapshot snapshot = AiDiagnosticsConfigSnapshot.from(config);

        assertEquals(AiConfigState.CONFIGURED, snapshot.chat().state());
        assertEquals("openrouter", snapshot.chat().provider());
        assertEquals("openrouter.ai", snapshot.chat().endpointHost());
        assertTrue(snapshot.chat().credentialConfigured());
        assertEquals("json_base64", snapshot.stt().format());
        assertEquals(AiConfigState.CONFIGURED, snapshot.stt().state());
        assertEquals(AiConfigState.DISABLED, snapshot.tts().state());
        assertEquals("pcm", snapshot.tts().format());

        String rendered = snapshot.toString();
        assertFalse(rendered.contains("SECRET_SENTINEL"));
        assertFalse(rendered.contains("STT_SECRET_SENTINEL"));
        assertFalse(rendered.contains("TTS_SECRET_SENTINEL"));
        assertFalse(rendered.contains("ENDPOINT_SECRET"));
        assertFalse(rendered.contains("QUERY_SECRET"));
    }

    @Test
    void enabledStageWithoutCredentialIsMisconfigured() {
        LivingWorldConfig config = new LivingWorldConfig();
        config.provider = "openrouter";
        config.apiKey = "";
        config.voiceInputEnabled = true;
        config.sttApiKey = "";

        AiDiagnosticsConfigSnapshot snapshot = AiDiagnosticsConfigSnapshot.from(config);

        assertEquals(AiConfigState.MISCONFIGURED, snapshot.chat().state());
        assertEquals(AiConfigState.MISCONFIGURED, snapshot.stt().state());
    }

    @Test
    void malformedEndpointIsMisconfiguredAndFailsSoftWithoutEchoingInput() {
        LivingWorldConfig config = new LivingWorldConfig();
        config.endpoint = "not a url SECRET_ENDPOINT";
        config.apiKey = "SECRET_SENTINEL";

        AiDiagnosticsConfigSnapshot snapshot = AiDiagnosticsConfigSnapshot.from(config);

        assertEquals(AiConfigState.MISCONFIGURED, snapshot.chat().state());
        assertEquals("<invalid>", snapshot.chat().endpointHost());
        assertFalse(snapshot.toString().contains("SECRET_ENDPOINT"));
        assertFalse(snapshot.toString().contains("SECRET_SENTINEL"));
    }
}
