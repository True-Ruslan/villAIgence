package net.conczin.mca.livingworld.voice;

/** Immutable provider-neutral text-to-speech request. */
public record TtsRequest(String text, String voiceId, TtsVoiceStyle style) {
    public TtsRequest {
        text = text == null ? "" : text.trim();
        voiceId = voiceId == null ? "" : voiceId.trim();
        style = style == null ? TtsVoiceStyle.NEUTRAL : style;
        if (text.isBlank()) throw new IllegalArgumentException("TTS text is required");
    }
}
