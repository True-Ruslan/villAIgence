package net.conczin.mca.livingworld.voice;

/** Non-disableable runtime safety bounds for microphone capture. */
public final class VoiceCaptureLimits {
    public static final int MIN_SECONDS = 1;
    public static final int MAX_SECONDS = 60;
    public static final long MAX_ACTIVE_PCM_BYTES = 32L * 1024L * 1024L;

    private VoiceCaptureLimits() {
    }

    public static int clampSeconds(int configuredSeconds) {
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, configuredSeconds));
    }
}
