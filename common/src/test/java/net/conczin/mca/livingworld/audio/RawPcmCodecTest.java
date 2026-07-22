package net.conczin.mca.livingworld.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RawPcmCodecTest {
    @Test
    void decodesSignedPcm16LittleEndian() {
        byte[] bytes = {
                0x01, 0x00,
                (byte) 0xff, (byte) 0xff,
                0x00, (byte) 0x80,
                (byte) 0xff, 0x7f
        };

        PcmAudio audio = RawPcmCodec.decodePcm16Mono(bytes, 24_000);

        assertEquals(24_000, audio.sampleRate());
        assertArrayEquals(new short[]{1, -1, Short.MIN_VALUE, Short.MAX_VALUE}, audio.samples());
    }

    @Test
    void emptyPcmIsValid() {
        PcmAudio audio = RawPcmCodec.decodePcm16Mono(new byte[0], 24_000);
        assertEquals(24_000, audio.sampleRate());
        assertArrayEquals(new short[0], audio.samples());
    }

    @Test
    void rejectsOddByteCount() {
        assertThrows(IllegalArgumentException.class,
                () -> RawPcmCodec.decodePcm16Mono(new byte[]{1, 2, 3}, 24_000));
    }

    @Test
    void rejectsInvalidSampleRate() {
        assertThrows(IllegalArgumentException.class,
                () -> RawPcmCodec.decodePcm16Mono(new byte[]{0, 0}, 0));
    }
}
