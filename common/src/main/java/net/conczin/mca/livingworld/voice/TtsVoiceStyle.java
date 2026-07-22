package net.conczin.mca.livingworld.voice;

/** Provider-neutral TTS delivery hints. */
public record TtsVoiceStyle(String instructions, double speed) {
    public static final TtsVoiceStyle NEUTRAL = new TtsVoiceStyle("Use a natural conversational delivery.", 1.0D);

    public TtsVoiceStyle {
        instructions = instructions == null ? "" : instructions.trim();
        if (!Double.isFinite(speed)) speed = 1.0D;
        speed = Math.max(0.25D, Math.min(4.0D, speed));
    }
}
