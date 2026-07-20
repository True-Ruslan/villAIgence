package net.conczin.mca.livingworld.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WavCodecTest {
    @Test
    void pcm16MonoRoundTripsThroughWav() {
        short[] samples = new short[]{Short.MIN_VALUE, -1234, 0, 1234, Short.MAX_VALUE};

        byte[] wav = WavCodec.encodePcm16Mono(samples, 48_000);
        PcmAudio decoded = WavCodec.decodePcm16Mono(wav);

        assertEquals(48_000, decoded.sampleRate());
        assertArrayEquals(samples, decoded.samples());
    }

    @Test
    void resamplesMonoPcmToVoiceChatRate() {
        PcmAudio input = new PcmAudio(24_000, new short[]{0, 10_000, 20_000, 30_000});

        PcmAudio output = input.resampleTo(48_000);

        assertEquals(48_000, output.sampleRate());
        assertEquals(8, output.samples().length);
        assertEquals(0, output.samples()[0]);
        assertEquals(30_000, output.samples()[7]);
    }

    @Test
    void rejectsUnsupportedWavEncoding() {
        byte[] wav = WavCodec.encodePcm16Mono(new short[]{1, 2, 3}, 48_000);
        wav[20] = 3; // IEEE float instead of PCM
        wav[21] = 0;

        assertThrows(IllegalArgumentException.class, () -> WavCodec.decodePcm16Mono(wav));
    }
}
