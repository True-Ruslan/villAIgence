package net.conczin.mca.livingworld.audio;

import java.util.Objects;

/** Codec helpers for headerless signed PCM16 little-endian mono audio. */
public final class RawPcmCodec {
    private RawPcmCodec() {
    }

    public static PcmAudio decodePcm16Mono(byte[] bytes, int sampleRate) {
        Objects.requireNonNull(bytes, "bytes");
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive");
        }
        if ((bytes.length & 1) != 0) {
            throw new IllegalArgumentException("PCM16 payload must contain an even number of bytes");
        }

        short[] samples = new short[bytes.length / 2];
        for (int i = 0; i < bytes.length; i += 2) {
            int lo = bytes[i] & 0xff;
            int hi = bytes[i + 1];
            samples[i / 2] = (short) ((hi << 8) | lo);
        }
        return new PcmAudio(sampleRate, samples);
    }
}
