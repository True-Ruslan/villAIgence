package net.conczin.mca.livingworld.voice;

import net.conczin.mca.livingworld.audio.PcmAudio;

import java.io.IOException;

@FunctionalInterface
public interface TextToSpeechProvider {
    PcmAudio synthesize(String text) throws IOException;

    /**
     * Provider-neutral rich request. Implementations that do not support voice/style capabilities
     * safely fall back to text-only synthesis.
     */
    default PcmAudio synthesize(TtsRequest request) throws IOException {
        return synthesize(request.text());
    }
}
