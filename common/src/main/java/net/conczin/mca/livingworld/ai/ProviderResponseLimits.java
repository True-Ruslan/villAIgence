package net.conczin.mca.livingworld.ai;

/** Central byte limits for untrusted provider response bodies. */
public final class ProviderResponseLimits {
    /** Allows large structured/reasoning responses while remaining finite. */
    public static final int CHAT_JSON_BYTES = 8 * 1024 * 1024;
    /** Far above a normal dialogue transcript, including verbose provider metadata. */
    public static final int STT_JSON_BYTES = 4 * 1024 * 1024;
    /** About 23 minutes of 24 kHz mono PCM16, while limiting decode/resample heap peaks. */
    public static final int TTS_AUDIO_BYTES = 64 * 1024 * 1024;
    /** Large enough for useful diagnostics without accepting arbitrary provider dumps. */
    public static final int ERROR_BODY_BYTES = 256 * 1024;
    /** Verification responses are tiny, but retain generous compatibility headroom. */
    public static final int VERIFICATION_JSON_BYTES = 64 * 1024;

    private ProviderResponseLimits() {
    }
}
