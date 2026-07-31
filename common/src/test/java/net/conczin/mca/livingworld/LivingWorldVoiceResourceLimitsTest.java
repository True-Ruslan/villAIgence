package net.conczin.mca.livingworld;

import net.conczin.mca.livingworld.voice.VoiceCaptureLimits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LivingWorldVoiceResourceLimitsTest {
    @Test
    void missingConfigKeepsExistingDurationInsideRuntimeBound() {
        LivingWorldConfig config = LivingWorldConfig.parseJson("{\"version\":2}");

        assertEquals(20, config.voiceMaxSeconds);
        assertEquals(20, VoiceCaptureLimits.clampSeconds(config.voiceMaxSeconds));
        assertEquals(32L * 1024L * 1024L, VoiceCaptureLimits.MAX_ACTIVE_PCM_BYTES);
    }

    @Test
    void runtimeDurationIsAlwaysClampedToOneThroughSixtySeconds() {
        assertEquals(1, VoiceCaptureLimits.clampSeconds(-5));
        assertEquals(1, VoiceCaptureLimits.clampSeconds(0));
        assertEquals(1, VoiceCaptureLimits.clampSeconds(1));
        assertEquals(60, VoiceCaptureLimits.clampSeconds(60));
        assertEquals(60, VoiceCaptureLimits.clampSeconds(9999));
    }

    @Test
    void extremeConfiguredDurationCannotExpandRuntimeCapture() {
        LivingWorldConfig config = LivingWorldConfig.parseJson(
                "{\"version\":2,\"voiceMaxSeconds\":2147483647}"
        );

        assertEquals(60, VoiceCaptureLimits.clampSeconds(config.voiceMaxSeconds));
    }
}
