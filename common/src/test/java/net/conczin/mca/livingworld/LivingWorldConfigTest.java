package net.conczin.mca.livingworld;

import net.conczin.mca.livingworld.voice.NpcVoiceCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingWorldConfigTest {
    @Test
    void defaultsUseVoiceInputWithTextOnlyNpcReplies() {
        LivingWorldConfig config = new LivingWorldConfig();

        assertEquals(2, config.version);
        assertTrue(config.enabled);
        assertEquals("openai", config.provider);
        assertEquals("https://api.openai.com/v1/chat/completions", config.endpoint);
        assertEquals("gpt-4.1-mini", config.model);
        assertTrue(config.safeActionsEnabled);
        assertTrue(config.persistentMemoryEnabled);
        assertEquals(16, config.persistentMemoryMaxMessages);
        assertEquals(1200, config.persistentMemoryMaxCharsPerMessage);
        assertTrue(config.eventMemoryEnabled);
        assertEquals(512, config.eventMemoryMaxEvents);
        assertEquals(72_000L, config.eventMemoryMaxAgeTicks);
        assertEquals(32.0D, config.eventContextRadius);
        assertEquals(8, config.eventContextMaxEvents);
        assertTrue(config.relationshipStateEnabled);
        assertEquals(2, config.relationshipMaxDeltaPerTurn);
        assertTrue(config.voiceInputEnabled);
        assertFalse(config.voiceOutputEnabled);
        assertEquals("auto", config.sttRequestFormat);
        assertEquals("", config.sttApiKey);
        assertEquals("https://api.openai.com/v1/audio/transcriptions", config.sttEndpoint);
        assertEquals("gpt-4o-mini-transcribe", config.sttModel);
        assertEquals("https://api.openai.com/v1/audio/speech", config.ttsEndpoint);
        assertEquals("tts-1", config.ttsModel);
        assertEquals("marin", config.ttsVoice);
        assertEquals("", config.ttsApiKey);
        assertEquals(800, config.voiceSilenceMillis);
        assertEquals(20, config.voiceMaxSeconds);
        assertEquals(10, config.connectTimeoutSeconds);
        assertEquals(60, config.readTimeoutSeconds);
        assertFalse(config.isConfiguredWithKey(""));
        assertTrue(config.isConfiguredWithKey("sk-test"));

        NpcVoiceCatalog.VoicePools pools = config.voicePools();
        assertFalse(pools.maleChild().isEmpty());
        assertFalse(pools.femaleChild().isEmpty());
        assertFalse(pools.maleTeen().isEmpty());
        assertFalse(pools.femaleTeen().isEmpty());
        assertFalse(pools.maleAdult().isEmpty());
        assertFalse(pools.femaleAdult().isEmpty());
        assertEquals("marin", pools.legacyFallback());
    }

    @Test
    void voicePoolsNormalizeBlanksDuplicatesAndKeepLegacyTtsVoiceFallback() {
        LivingWorldConfig config = LivingWorldConfig.parseJson("""
                {
                  "version": 2,
                  "ttsVoice": "legacy-voice",
                  "maleChildVoices": [" ash ", "", "ash", null],
                  "femaleChildVoices": [],
                  "globalVoiceFallbacks": [" alloy ", "alloy", "verse"]
                }
                """);

        NpcVoiceCatalog.VoicePools pools = config.voicePools();
        assertEquals(List.of("ash"), pools.maleChild());
        assertEquals(List.of(), pools.femaleChild());
        assertEquals(List.of("alloy", "verse"), pools.globalFallback());
        assertEquals("legacy-voice", pools.legacyFallback());
    }

    @Test
    void providerSpecificEnvironmentKeyWinsOverConfiguredKey() {
        assertEquals("sk-or-env", LivingWorldConfig.resolveProviderApiKey("openrouter", "sk-or-env", "sk-oa-env", "sk-file"));
        assertEquals("sk-oa-env", LivingWorldConfig.resolveProviderApiKey("openai", "sk-or-env", "sk-oa-env", "sk-file"));
        assertEquals("sk-file", LivingWorldConfig.resolveProviderApiKey("openrouter", "  ", "sk-oa-env", "sk-file"));
        assertEquals("", LivingWorldConfig.resolveProviderApiKey("openrouter", null, null, null));
    }

    @Test
    void dedicatedSttKeySupportsOpenRouterWithoutChangingChatProvider() {
        assertEquals("sk-or-env", LivingWorldConfig.resolveSttApiKey(
                "https://openrouter.ai/api/v1/audio/transcriptions", "sk-or-env", "sk-stt-file", "sk-main"));
        assertEquals("sk-stt-file", LivingWorldConfig.resolveSttApiKey(
                "https://openrouter.ai/api/v1/audio/transcriptions", "", "sk-stt-file", "sk-main"));
        assertEquals("sk-main", LivingWorldConfig.resolveSttApiKey(
                "https://api.openai.com/v1/audio/transcriptions", "sk-or-env", "", "sk-main"));
    }

    @Test
    void ttsCredentialIsResolvedIndependentlyFromChatProvider() {
        assertEquals("sk-openai-env", LivingWorldConfig.resolveTtsApiKey(
                "https://api.openai.com/v1/audio/speech", "sk-openai-env", "sk-tts-file", "openrouter", "sk-openrouter-main"));
        assertEquals("sk-tts-file", LivingWorldConfig.resolveTtsApiKey(
                "https://example-tts.invalid/v1/speech", "sk-openai-env", "sk-tts-file", "openrouter", "sk-main"));
        assertEquals("sk-main", LivingWorldConfig.resolveTtsApiKey(
                "https://example-tts.invalid/v1/speech", "", "", "openrouter", "sk-main"));
        assertEquals("", LivingWorldConfig.resolveTtsApiKey(
                "https://api.openai.com/v1/audio/speech", "", "", "openrouter", "sk-openrouter-main"));
        assertEquals("sk-openai-main", LivingWorldConfig.resolveTtsApiKey(
                "https://api.openai.com/v1/audio/speech", "", "", "openai", "sk-openai-main"));
        assertEquals("sk-tts-file", LivingWorldConfig.resolveTtsApiKey(
                "https://evil.example/proxy/api.openai.com/v1/audio/speech", "sk-openai-env", "sk-tts-file", "openrouter", "sk-main"));
    }

    @Test
    void openRouterIsAcceptedAsOpenAiCompatibleProvider() {
        LivingWorldConfig config = new LivingWorldConfig();
        config.provider = "openrouter";
        assertTrue(config.isConfiguredWithKey("sk-or-test"));
    }

    @Test
    void legacyVoiceEnabledTrueMigratesToFullVoice() {
        LivingWorldConfig config = LivingWorldConfig.parseJson("""
                {
                  "version": 1,
                  "voiceEnabled": true
                }
                """);

        assertEquals(2, config.version);
        assertTrue(config.voiceInputEnabled);
        assertTrue(config.voiceOutputEnabled);
    }

    @Test
    void legacyVoiceEnabledFalseMigratesToNoVoice() {
        LivingWorldConfig config = LivingWorldConfig.parseJson("""
                {
                  "version": 1,
                  "voiceEnabled": false
                }
                """);

        assertEquals(2, config.version);
        assertFalse(config.voiceInputEnabled);
        assertFalse(config.voiceOutputEnabled);
    }

    @Test
    void versionTwoPreservesIndependentVoiceFlags() {
        LivingWorldConfig config = LivingWorldConfig.parseJson("""
                {
                  "version": 2,
                  "voiceInputEnabled": true,
                  "voiceOutputEnabled": false,
                  "sttRequestFormat": "json_base64"
                }
                """);

        assertTrue(config.voiceInputEnabled);
        assertFalse(config.voiceOutputEnabled);
        assertEquals("json_base64", config.sttRequestFormat);
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
