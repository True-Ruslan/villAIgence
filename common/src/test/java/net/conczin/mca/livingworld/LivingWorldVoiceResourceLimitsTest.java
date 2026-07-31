package net.conczin.mca.livingworld;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LivingWorldVoiceResourceLimitsTest {
    private static final int MIB = 1024 * 1024;

    @Test
    void missingFieldsKeepSecureOperationalDefaults() {
        LivingWorldConfig config = LivingWorldConfig.parseJson("{\"version\":2}");

        assertEquals(20, config.voiceMaxSeconds);
        assertEquals(32 * MIB, config.voiceMaxActivePcmBytes);
    }

    @Test
    void voiceDurationIsClampedToOneThroughSixtySeconds() {
        assertEquals(1, LivingWorldConfig.parseJson(
                "{\"version\":2,\"voiceMaxSeconds\":-5}"
        ).voiceMaxSeconds);
        assertEquals(1, LivingWorldConfig.parseJson(
                "{\"version\":2,\"voiceMaxSeconds\":0}"
        ).voiceMaxSeconds);
        assertEquals(1, LivingWorldConfig.parseJson(
                "{\"version\":2,\"voiceMaxSeconds\":1}"
        ).voiceMaxSeconds);
        assertEquals(60, LivingWorldConfig.parseJson(
                "{\"version\":2,\"voiceMaxSeconds\":60}"
        ).voiceMaxSeconds);
        assertEquals(60, LivingWorldConfig.parseJson(
                "{\"version\":2,\"voiceMaxSeconds\":9999}"
        ).voiceMaxSeconds);
    }

    @Test
    void aggregatePcmBudgetIsClampedToOneThroughTwoHundredFiftySixMiB() {
        assertEquals(MIB, LivingWorldConfig.parseJson(
                "{\"version\":2,\"voiceMaxActivePcmBytes\":0}"
        ).voiceMaxActivePcmBytes);
        assertEquals(MIB, LivingWorldConfig.parseJson(
                "{\"version\":2,\"voiceMaxActivePcmBytes\":1024}"
        ).voiceMaxActivePcmBytes);
        assertEquals(64 * MIB, LivingWorldConfig.parseJson(
                "{\"version\":2,\"voiceMaxActivePcmBytes\":67108864}"
        ).voiceMaxActivePcmBytes);
        assertEquals(256 * MIB, LivingWorldConfig.parseJson(
                "{\"version\":2,\"voiceMaxActivePcmBytes\":2147483647}"
        ).voiceMaxActivePcmBytes);
    }
}
