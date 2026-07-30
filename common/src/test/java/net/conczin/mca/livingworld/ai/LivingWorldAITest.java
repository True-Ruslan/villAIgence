package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingWorldAITest {
    @Test
    void configuredLivingWorldSettingsWinOverLegacyMcaSettings() {
        AiProviderSettings livingWorld = new AiProviderSettings(
                ProviderEndpointPolicy.parse("https://api.openai.com/v1/chat/completions", false),
                "gpt-4.1-mini",
                "sk-test",
                10_000,
                60_000,
                false,
                false
        );
        AiProviderSettings legacy = new AiProviderSettings(
                ProviderEndpointPolicy.parse("https://legacy.example/chat", false),
                "legacy-model",
                "legacy-token",
                10_000,
                60_000,
                false,
                true
        );

        assertSame(livingWorld, LivingWorldAI.selectSettings(true, livingWorld, legacy));
        assertSame(legacy, LivingWorldAI.selectSettings(false, livingWorld, legacy));
        assertFalse(livingWorld.includeMessageNames());
        assertTrue(legacy.includeMessageNames());
        assertEquals("api.openai.com", livingWorld.endpoint().host());
    }

    @Test
    void trustedLegacyHostUsesNormalizedHostBoundary() {
        ProviderEndpoint trusted = ProviderEndpointPolicy.parse("https://api.conczin.net/chat", false);
        ProviderEndpoint lookalike = ProviderEndpointPolicy.parse("https://conczin.net.example.invalid/chat", false);

        assertTrue(LivingWorldAI.shouldUsePlayerNameAsToken(trusted, ""));
        assertFalse(LivingWorldAI.shouldUsePlayerNameAsToken(lookalike, "legacy-token"));
        assertFalse(lookalike.trustedConczin());
    }

    @Test
    void timeoutSecondsAreConvertedSafely() {
        assertEquals(1_000, AiProviderSettings.secondsToMillis(1));
        assertEquals(10_000, AiProviderSettings.secondsToMillis(10));
        assertEquals(1_000, AiProviderSettings.secondsToMillis(0));
        assertEquals(Integer.MAX_VALUE, AiProviderSettings.secondsToMillis(Integer.MAX_VALUE));
    }
}
