package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LivingWorldAITest {
    @Test
    void configuredLivingWorldSettingsWinOverLegacyMcaSettings() {
        AiProviderSettings livingWorld = new AiProviderSettings(
                "https://api.openai.com/v1/chat/completions",
                "gpt-4.1-mini",
                "sk-test",
                10_000,
                60_000,
                false
        );
        AiProviderSettings legacy = new AiProviderSettings(
                "https://legacy.example/chat",
                "legacy-model",
                "legacy-token",
                10_000,
                60_000,
                false
        );

        assertSame(livingWorld, LivingWorldAI.selectSettings(true, livingWorld, legacy));
        assertSame(legacy, LivingWorldAI.selectSettings(false, livingWorld, legacy));
    }

    @Test
    void timeoutSecondsAreConvertedSafely() {
        assertEquals(1_000, AiProviderSettings.secondsToMillis(1));
        assertEquals(10_000, AiProviderSettings.secondsToMillis(10));
        assertEquals(1_000, AiProviderSettings.secondsToMillis(0));
        assertEquals(Integer.MAX_VALUE, AiProviderSettings.secondsToMillis(Integer.MAX_VALUE));
    }
}
