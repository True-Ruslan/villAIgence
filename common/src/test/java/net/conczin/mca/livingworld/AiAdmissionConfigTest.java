package net.conczin.mca.livingworld;

import net.conczin.mca.livingworld.admission.AiAdmissionSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiAdmissionConfigTest {
    @Test
    void defaultsAreSafeAndBounded() {
        LivingWorldConfig config = new LivingWorldConfig();

        assertEquals(4, config.aiChatMaxConcurrentRequests);
        assertEquals(2, config.aiSttMaxConcurrentRequests);
        assertEquals(2, config.aiTtsMaxConcurrentRequests);
        assertEquals(750, config.aiPerPlayerCooldownMillis);
        assertEquals(5_000, config.aiProviderRateLimitCooldownMillis);

        AiAdmissionSettings settings = AiAdmissionSettings.from(config);
        assertEquals(4, settings.chatMaxConcurrentRequests());
        assertEquals(2, settings.sttMaxConcurrentRequests());
        assertEquals(2, settings.ttsMaxConcurrentRequests());
        assertEquals(750, settings.perPlayerCooldownMillis());
        assertEquals(5_000, settings.providerRateLimitCooldownMillis());
    }

    @Test
    void existingVersionTwoConfigWithoutAdmissionFieldsKeepsDefaults() {
        LivingWorldConfig config = LivingWorldConfig.parseJson("""
                {
                  "version": 2,
                  "voiceInputEnabled": true,
                  "voiceOutputEnabled": false
                }
                """);

        assertEquals(4, config.aiChatMaxConcurrentRequests);
        assertEquals(2, config.aiSttMaxConcurrentRequests);
        assertEquals(2, config.aiTtsMaxConcurrentRequests);
        assertEquals(750, config.aiPerPlayerCooldownMillis);
        assertEquals(5_000, config.aiProviderRateLimitCooldownMillis);
    }

    @Test
    void invalidAdmissionLimitsAreClampedDuringNormalization() {
        LivingWorldConfig config = LivingWorldConfig.parseJson("""
                {
                  "version": 2,
                  "aiChatMaxConcurrentRequests": 0,
                  "aiSttMaxConcurrentRequests": -4,
                  "aiTtsMaxConcurrentRequests": 999,
                  "aiPerPlayerCooldownMillis": -1,
                  "aiProviderRateLimitCooldownMillis": 9999999
                }
                """);

        assertEquals(1, config.aiChatMaxConcurrentRequests);
        assertEquals(1, config.aiSttMaxConcurrentRequests);
        assertEquals(64, config.aiTtsMaxConcurrentRequests);
        assertEquals(0, config.aiPerPlayerCooldownMillis);
        assertEquals(300_000, config.aiProviderRateLimitCooldownMillis);
    }
}
