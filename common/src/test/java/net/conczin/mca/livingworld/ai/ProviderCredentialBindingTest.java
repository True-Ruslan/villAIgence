package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderCredentialBindingTest {
    private static final ProviderEndpoint OPENAI = ProviderEndpointPolicy.parse(
            "https://api.openai.com/v1/chat/completions",
            false
    );
    private static final ProviderEndpoint OPENROUTER = ProviderEndpointPolicy.parse(
            "https://openrouter.ai/api/v1/chat/completions",
            false
    );
    private static final ProviderEndpoint CUSTOM = ProviderEndpointPolicy.parse(
            "https://provider.example/v1/chat/completions",
            false
    );

    @Test
    void chatEnvironmentKeysAreSelectedByValidatedEndpointFamily() {
        assertEquals("sk-openai-env", ProviderCredentialBinding.resolveChatKey(
                OPENAI,
                "sk-configured",
                "sk-openai-env",
                "sk-openrouter-env"
        ));
        assertEquals("sk-openrouter-env", ProviderCredentialBinding.resolveChatKey(
                OPENROUTER,
                "sk-configured",
                "sk-openai-env",
                "sk-openrouter-env"
        ));
    }

    @Test
    void customChatEndpointIgnoresProviderEnvironmentKeys() {
        assertEquals("sk-custom", ProviderCredentialBinding.resolveChatKey(
                CUSTOM,
                " sk-custom ",
                "sk-openai-env",
                "sk-openrouter-env"
        ));
        assertEquals("", ProviderCredentialBinding.resolveChatKey(
                CUSTOM,
                "",
                "sk-openai-env",
                "sk-openrouter-env"
        ));
    }

    @Test
    void standardAudioEndpointUsesFamilyEnvironmentThenDedicatedThenCompatibleMainKey() {
        assertEquals("sk-openai-env", ProviderCredentialBinding.resolveAudioKey(
                OPENAI,
                "sk-dedicated",
                OPENAI,
                "sk-main",
                "sk-openai-env",
                "sk-openrouter-env"
        ));
        assertEquals("sk-dedicated", ProviderCredentialBinding.resolveAudioKey(
                OPENAI,
                " sk-dedicated ",
                OPENAI,
                "sk-main",
                "",
                "sk-openrouter-env"
        ));
        assertEquals("sk-main", ProviderCredentialBinding.resolveAudioKey(
                OPENAI,
                "",
                OPENAI,
                " sk-main ",
                "",
                "sk-openrouter-env"
        ));
        assertEquals("", ProviderCredentialBinding.resolveAudioKey(
                OPENAI,
                "",
                OPENROUTER,
                "sk-openrouter-main",
                "",
                ""
        ));
    }

    @Test
    void customAudioEndpointRequiresDedicatedCredential() {
        assertEquals("sk-audio", ProviderCredentialBinding.resolveAudioKey(
                CUSTOM,
                " sk-audio ",
                OPENAI,
                "sk-main",
                "sk-openai-env",
                "sk-openrouter-env"
        ));
        assertEquals("", ProviderCredentialBinding.resolveAudioKey(
                CUSTOM,
                "",
                OPENAI,
                "sk-main",
                "sk-openai-env",
                "sk-openrouter-env"
        ));
    }

    @Test
    void boundEndpointTrimsCredentialWithoutExposingItThroughEndpoint() {
        ProviderCredentialBinding.BoundEndpoint binding = new ProviderCredentialBinding.BoundEndpoint(
                CUSTOM,
                " sk-secret "
        );

        assertEquals(CUSTOM, binding.endpoint());
        assertEquals("sk-secret", binding.apiKey());
        assertEquals("https://provider.example/v1/chat/completions", binding.endpoint().externalForm());
    }
}
