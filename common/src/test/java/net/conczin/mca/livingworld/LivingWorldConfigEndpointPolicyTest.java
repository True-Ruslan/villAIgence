package net.conczin.mca.livingworld;

import net.conczin.mca.livingworld.ai.ProviderCredentialBinding;
import net.conczin.mca.livingworld.ai.ProviderEndpoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingWorldConfigEndpointPolicyTest {
    @Test
    void insecureLoopbackDevelopmentModeIsDisabledByDefault() {
        LivingWorldConfig config = new LivingWorldConfig();

        assertFalse(config.allowInsecureLoopbackAiEndpoints);
    }

    @Test
    void customChatEndpointUsesOnlyItsConfiguredCredential() {
        LivingWorldConfig config = new LivingWorldConfig();
        config.endpoint = "https://provider.example/v1/chat/completions";
        config.apiKey = " sk-custom ";

        ProviderCredentialBinding.BoundEndpoint binding = config.resolvedChatEndpoint();

        assertEquals(ProviderEndpoint.Family.CUSTOM, binding.endpoint().family());
        assertEquals("provider.example", binding.endpoint().host());
        assertEquals("sk-custom", binding.apiKey());
        assertTrue(config.isConfigured());
    }

    @Test
    void unsafeRemoteHttpChatEndpointFailsClosed() {
        LivingWorldConfig config = new LivingWorldConfig();
        config.endpoint = "http://provider.example/v1/chat/completions";
        config.apiKey = "sk-custom";
        config.allowInsecureLoopbackAiEndpoints = true;

        assertFalse(config.isConfigured());
    }

    @Test
    void loopbackHttpRequiresExplicitOptIn() {
        LivingWorldConfig config = new LivingWorldConfig();
        config.endpoint = "http://127.0.0.1:11434/v1/chat/completions";
        config.apiKey = "local-key";

        assertFalse(config.isConfigured());

        config.allowInsecureLoopbackAiEndpoints = true;
        assertTrue(config.isConfigured());
        assertTrue(config.resolvedChatEndpoint().endpoint().loopback());
    }

    @Test
    void customAudioEndpointsRequireDedicatedKeys() {
        LivingWorldConfig config = new LivingWorldConfig();
        config.endpoint = "https://api.openai.com/v1/chat/completions";
        config.apiKey = "sk-main";
        config.sttEndpoint = "https://speech.example/v1/transcriptions";
        config.ttsEndpoint = "https://speech.example/v1/speech";
        config.sttApiKey = "";
        config.ttsApiKey = "";

        assertEquals("", config.resolvedSttEndpoint().apiKey());
        assertEquals("", config.resolvedTtsEndpoint().apiKey());

        config.sttApiKey = "sk-stt";
        config.ttsApiKey = "sk-tts";
        assertEquals("sk-stt", config.resolvedSttEndpoint().apiKey());
        assertEquals("sk-tts", config.resolvedTtsEndpoint().apiKey());
    }

    @Test
    void standardAudioEndpointMayReuseCompatibleMainCredential() {
        LivingWorldConfig config = new LivingWorldConfig();
        config.endpoint = "https://api.openai.com/v1/chat/completions";
        config.apiKey = "sk-main";
        config.sttEndpoint = "https://api.openai.com/v1/audio/transcriptions";
        config.ttsEndpoint = "https://api.openai.com/v1/audio/speech";

        assertEquals("sk-main", config.resolvedSttEndpoint().apiKey());
        assertEquals("sk-main", config.resolvedTtsEndpoint().apiKey());
    }
}
