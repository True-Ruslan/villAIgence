package net.conczin.mca.livingworld.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoicePcmBudgetAcceptanceProbeTest {
    @Test
    void concurrentProbeRejectsOverflowAndFullyRecovers() throws Exception {
        VoicePcmBudgetAcceptanceProbe.Result result =
                VoicePcmBudgetAcceptanceProbe.run(16L, 10, 4L);

        assertEquals(VoiceCaptureLimits.MIN_SECONDS, result.clampedLowSeconds());
        assertEquals(VoiceCaptureLimits.MAX_SECONDS, result.clampedHighSeconds());
        assertEquals(4, result.accepted());
        assertEquals(6, result.rejected());
        assertEquals(16L, result.peakBytes());
        assertEquals(0L, result.finalBytes());
        assertTrue(result.recoveryReservationSucceeded());
        assertTrue(result.passed());
        assertTrue(result.toJson().contains("VILLAIGENCE_PCM_PROBE_PASS"));
        assertTrue(result.toJson().contains("\"clampedLowSeconds\":1"));
        assertTrue(result.toJson().contains("\"clampedHighSeconds\":120"));
    }

    @Test
    void probeHandlesCapacityLargerThanWorkerDemand() throws Exception {
        VoicePcmBudgetAcceptanceProbe.Result result =
                VoicePcmBudgetAcceptanceProbe.run(100L, 3, 10L);

        assertEquals(3, result.accepted());
        assertEquals(0, result.rejected());
        assertEquals(30L, result.peakBytes());
        assertEquals(0L, result.finalBytes());
        assertTrue(result.recoveryReservationSucceeded());
        assertTrue(result.passed());
    }

    @Test
    void invalidProbeArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                VoicePcmBudgetAcceptanceProbe.run(0L, 1, 1L));
        assertThrows(IllegalArgumentException.class, () ->
                VoicePcmBudgetAcceptanceProbe.run(1L, 0, 1L));
        assertThrows(IllegalArgumentException.class, () ->
                VoicePcmBudgetAcceptanceProbe.run(1L, 513, 1L));
        assertThrows(IllegalArgumentException.class, () ->
                VoicePcmBudgetAcceptanceProbe.run(1L, 1, 0L));
        assertThrows(IllegalArgumentException.class, () ->
                VoicePcmBudgetAcceptanceProbe.run(4L, 1, 5L));
    }
}
