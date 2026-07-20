package net.conczin.mca.livingworld;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingWorldConfigTest {
    @Test
    void defaultsRequireOnlyAnApiKeyForOpenAiMvp() {
        LivingWorldConfig config = new LivingWorldConfig();

        assertTrue(config.enabled);
        assertEquals("openai", config.provider);
        assertEquals("https://api.openai.com/v1/chat/completions", config.endpoint);
        assertEquals("gpt-4.1-mini", config.model);
        assertTrue(config.voiceEnabled);
        assertEquals("https://api.openai.com/v1/audio/transcriptions", config.sttEndpoint);
        assertEquals("gpt-4o-mini-transcribe", config.sttModel);
        assertEquals("https://api.openai.com/v1/audio/speech", config.ttsEndpoint);
        assertEquals("gpt-4o-mini-tts", config.ttsModel);
        assertEquals("marin", config.ttsVoice);
        assertEquals(800, config.voiceSilenceMillis);
        assertEquals(20, config.voiceMaxSeconds);
        assertEquals(10, config.connectTimeoutSeconds);
        assertEquals(60, config.readTimeoutSeconds);
        assertFalse(config.isConfiguredWithKey(""));
        assertTrue(config.isConfiguredWithKey("sk-test"));
    }

    @Test
    void environmentKeyWinsOverFileKey() {
        assertEquals("sk-env", LivingWorldConfig.resolveApiKey("sk-env", "sk-file"));
        assertEquals("sk-file", LivingWorldConfig.resolveApiKey("  ", "sk-file"));
        assertEquals("", LivingWorldConfig.resolveApiKey(null, null));
    }

    @Test
    void disabledOrUnsupportedProviderDoesNotActivateLivingWorld() {
        LivingWorldConfig disabled = new LivingWorldConfig();
        disabled.enabled = false;
        assertFalse(disabled.isConfiguredWithKey("sk-test"));

        LivingWorldConfig unsupported = new LivingWorldConfig();
        unsupported.provider = "unknown";
        assertFalse(unsupported.isConfiguredWithKey("sk-test"));
    }
}
