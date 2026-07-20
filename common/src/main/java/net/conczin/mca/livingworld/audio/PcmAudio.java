package net.conczin.mca.livingworld.audio;

import java.util.Objects;

/**
 * Mono signed 16-bit PCM audio.
 */
public record PcmAudio(int sampleRate, short[] samples) {
    public PcmAudio {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive");
        }
        Objects.requireNonNull(samples, "samples");
    }

    public PcmAudio resampleTo(int targetSampleRate) {
        if (targetSampleRate <= 0) {
            throw new IllegalArgumentException("targetSampleRate must be positive");
        }
        if (targetSampleRate == sampleRate || samples.length == 0) {
            return new PcmAudio(targetSampleRate, samples.clone());
        }

        int outputLength = Math.max(1, (int) Math.round(samples.length * (double) targetSampleRate / sampleRate));
        short[] output = new short[outputLength];
        double sourceStep = sampleRate / (double) targetSampleRate;

        for (int i = 0; i < outputLength; i++) {
            double sourcePosition = Math.min(samples.length - 1.0, i * sourceStep);
            int left = (int) Math.floor(sourcePosition);
            int right = Math.min(samples.length - 1, left + 1);
            double fraction = sourcePosition - left;
            double interpolated = samples[left] + (samples[right] - samples[left]) * fraction;
            output[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(interpolated)));
        }

        return new PcmAudio(targetSampleRate, output);
    }
}
