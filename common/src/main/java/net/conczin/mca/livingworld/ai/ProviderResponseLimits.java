package net.conczin.mca.livingworld.ai;

/** Central byte limits for untrusted provider response bodies. */
public final class ProviderResponseLimits {
    public static final int CHAT_JSON_BYTES = 1024 * 1024;
    public static final int STT_JSON_BYTES = 512 * 1024;
    public static final int TTS_AUDIO_BYTES = 32 * 1024 * 1024;
    public static final int ERROR_BODY_BYTES = 64 * 1024;

    private ProviderResponseLimits() {
    }
}
