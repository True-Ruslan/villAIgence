package net.conczin.mca.livingworld.ai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Reads untrusted provider bodies with a hard streaming byte limit. */
public final class BoundedResponseReader {
    private static final int BUFFER_BYTES = 8 * 1024;

    private BoundedResponseReader() {
    }

    public static byte[] readBytes(InputStream input, long declaredLength, int maxBytes) throws IOException {
        Objects.requireNonNull(input, "input");
        if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be positive");
        if (declaredLength > maxBytes) {
            throw new ResponseTooLargeException(maxBytes, declaredLength);
        }

        int initialCapacity = declaredLength >= 0L
                ? (int) Math.min(declaredLength, maxBytes)
                : Math.min(BUFFER_BYTES, maxBytes);
        ByteArrayOutputStream output = new ByteArrayOutputStream(initialCapacity);
        byte[] buffer = new byte[Math.min(BUFFER_BYTES, maxBytes)];
        long total = 0L;

        while (true) {
            long remainingPlusOne = (long) maxBytes - total + 1L;
            int requested = (int) Math.min(buffer.length, remainingPlusOne);
            int read = input.read(buffer, 0, requested);
            if (read < 0) break;
            if (read == 0) {
                int value = input.read();
                if (value < 0) break;
                total++;
                if (total > maxBytes) throw new ResponseTooLargeException(maxBytes, total);
                output.write(value);
                continue;
            }

            total += read;
            if (total > maxBytes) throw new ResponseTooLargeException(maxBytes, total);
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    public static String readUtf8(InputStream input, long declaredLength, int maxBytes) throws IOException {
        return new String(readBytes(input, declaredLength, maxBytes), StandardCharsets.UTF_8);
    }

    public static final class ResponseTooLargeException extends IOException {
        private final int limitBytes;
        private final long observedBytes;

        public ResponseTooLargeException(int limitBytes, long observedBytes) {
            super("Provider response exceeded byte limit: limit=" + limitBytes + ", observed=" + observedBytes);
            this.limitBytes = limitBytes;
            this.observedBytes = observedBytes;
        }

        public int limitBytes() {
            return limitBytes;
        }

        public long observedBytes() {
            return observedBytes;
        }
    }
}
