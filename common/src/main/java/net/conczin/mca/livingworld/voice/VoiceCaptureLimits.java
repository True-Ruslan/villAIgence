package net.conczin.mca.livingworld.voice;

/** Non-disableable runtime safety bounds for microphone capture. */
public final class VoiceCaptureLimits {
    public static final int MIN_SECONDS = 1;
    /** Supports unusually long continuous speech without permitting unbounded capture. */
    public static final int MAX_SECONDS = 120;
    /** Roughly eleven simultaneous 120-second captures at 48 kHz mono PCM16. */
    public static final long MAX_ACTIVE_PCM_BYTES = 128L * 1024L * 1024L;

    private VoiceCaptureLimits() {
    }

    public static int clampSeconds(int configuredSeconds) {
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, configuredSeconds));
    }
}
