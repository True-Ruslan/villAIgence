package net.conczin.mca.livingworld.voice;

import net.conczin.mca.livingworld.audio.PcmAudio;

import java.io.IOException;

@FunctionalInterface
public interface TextToSpeechProvider {
    PcmAudio synthesize(String text) throws IOException;
}
