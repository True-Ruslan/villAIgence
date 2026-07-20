package net.conczin.mca.livingworld.audio;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal RIFF/WAVE codec for mono 16-bit PCM used by the OpenAI audio endpoints.
 */
public final class WavCodec {
    private static final int PCM_FORMAT = 1;
    private static final int MONO = 1;
    private static final int BITS_PER_SAMPLE = 16;

    private WavCodec() {
    }

    public static byte[] encodePcm16Mono(short[] samples, int sampleRate) {
        if (samples == null) throw new IllegalArgumentException("samples must not be null");
        if (sampleRate <= 0) throw new IllegalArgumentException("sampleRate must be positive");

        int dataSize = Math.multiplyExact(samples.length, 2);
        ByteArrayOutputStream output = new ByteArrayOutputStream(44 + dataSize);
        writeAscii(output, "RIFF");
        writeIntLE(output, 36 + dataSize);
        writeAscii(output, "WAVE");
        writeAscii(output, "fmt ");
        writeIntLE(output, 16);
        writeShortLE(output, PCM_FORMAT);
        writeShortLE(output, MONO);
        writeIntLE(output, sampleRate);
        writeIntLE(output, sampleRate * 2);
        writeShortLE(output, 2);
        writeShortLE(output, BITS_PER_SAMPLE);
        writeAscii(output, "data");
        writeIntLE(output, dataSize);
        for (short sample : samples) {
            writeShortLE(output, sample);
        }
        return output.toByteArray();
    }

    public static PcmAudio decodePcm16Mono(byte[] wav) {
        if (wav == null || wav.length < 44) {
            throw new IllegalArgumentException("Invalid WAV data");
        }
        if (!matches(wav, 0, "RIFF") || !matches(wav, 8, "WAVE")) {
            throw new IllegalArgumentException("Not a RIFF/WAVE file");
        }

        Integer sampleRate = null;
        int dataOffset = -1;
        int dataLength = -1;
        int offset = 12;
        while (offset + 8 <= wav.length) {
            String chunkId = new String(wav, offset, 4, StandardCharsets.US_ASCII);
            int chunkLength = readIntLE(wav, offset + 4);
            if (chunkLength < 0 || offset + 8L + chunkLength > wav.length) {
                throw new IllegalArgumentException("Invalid WAV chunk length");
            }
            int chunkData = offset + 8;

            if ("fmt ".equals(chunkId)) {
                if (chunkLength < 16) throw new IllegalArgumentException("Invalid WAV fmt chunk");
                int format = readUnsignedShortLE(wav, chunkData);
                int channels = readUnsignedShortLE(wav, chunkData + 2);
                int rate = readIntLE(wav, chunkData + 4);
                int bits = readUnsignedShortLE(wav, chunkData + 14);
                if (format != PCM_FORMAT || channels != MONO || bits != BITS_PER_SAMPLE || rate <= 0) {
                    throw new IllegalArgumentException("Only mono 16-bit PCM WAV is supported");
                }
                sampleRate = rate;
            } else if ("data".equals(chunkId)) {
                dataOffset = chunkData;
                dataLength = chunkLength;
            }

            offset = chunkData + chunkLength + (chunkLength & 1);
        }

        if (sampleRate == null || dataOffset < 0 || dataLength < 0 || (dataLength & 1) != 0) {
            throw new IllegalArgumentException("Incomplete WAV file");
        }

        short[] samples = new short[dataLength / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) readUnsignedShortLE(wav, dataOffset + i * 2);
        }
        return new PcmAudio(sampleRate, samples);
    }

    private static boolean matches(byte[] data, int offset, String value) {
        byte[] expected = value.getBytes(StandardCharsets.US_ASCII);
        if (offset < 0 || offset + expected.length > data.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if (data[offset + i] != expected[i]) return false;
        }
        return true;
    }

    private static int readUnsignedShortLE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    private static int readIntLE(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    private static void writeAscii(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeShortLE(ByteArrayOutputStream output, int value) {
        output.write(value & 0xFF);
        output.write((value >>> 8) & 0xFF);
    }

    private static void writeIntLE(ByteArrayOutputStream output, int value) {
        output.write(value & 0xFF);
        output.write((value >>> 8) & 0xFF);
        output.write((value >>> 16) & 0xFF);
        output.write((value >>> 24) & 0xFF);
    }
}
