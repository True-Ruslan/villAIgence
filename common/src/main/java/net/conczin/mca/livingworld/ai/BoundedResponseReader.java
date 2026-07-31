package net.conczin.mca.livingworld.ai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Reads untrusted provider bodies with hard streaming byte and total-read limits. */
public final class BoundedResponseReader {
    private static final int BUFFER_BYTES = 8 * 1024;
    /** Generous hard ceiling that prevents a slow-drip response from extending forever. */
    private static final long MAX_BODY_READ_NANOS = TimeUnit.MINUTES.toNanos(10);

    private BoundedResponseReader() {
    }

    public static byte[] readBytes(InputStream input, long declaredLength, int maxBytes) throws IOException {
        return readBytes(
                input,
                declaredLength,
                maxBytes,
                MAX_BODY_READ_NANOS,
                System::nanoTime
        );
    }

    static byte[] readBytes(
            InputStream input,
            long declaredLength,
            int maxBytes,
            long maxDurationNanos,
            LongSupplier nanoTime
    ) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(nanoTime, "nanoTime");
        if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be positive");
        if (maxDurationNanos <= 0L) {
            throw new IllegalArgumentException("maxDurationNanos must be positive");
        }
        if (declaredLength > maxBytes) {
            throw new ResponseTooLargeException(maxBytes, declaredLength);
        }

        int initialCapacity = declaredLength >= 0L
                ? (int) Math.min(declaredLength, maxBytes)
                : Math.min(BUFFER_BYTES, maxBytes);
        ByteArrayOutputStream output = new ByteArrayOutputStream(initialCapacity);
        byte[] buffer = new byte[Math.min(BUFFER_BYTES, maxBytes)];
        long total = 0L;
        long startedNanos = nanoTime.getAsLong();

        while (true) {
            checkDeadline(startedNanos, maxDurationNanos, nanoTime);
            long remainingPlusOne = (long) maxBytes - total + 1L;
            int requested = (int) Math.min(buffer.length, remainingPlusOne);
            int read = input.read(buffer, 0, requested);
            checkDeadline(startedNanos, maxDurationNanos, nanoTime);
            if (read < 0) break;
            if (read == 0) {
                int value = input.read();
                checkDeadline(startedNanos, maxDurationNanos, nanoTime);
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

    private static void checkDeadline(
            long startedNanos,
            long maxDurationNanos,
            LongSupplier nanoTime
    ) throws ResponseDeadlineExceededException {
        long elapsedNanos = nanoTime.getAsLong() - startedNanos;
        if (elapsedNanos >= maxDurationNanos) {
            throw new ResponseDeadlineExceededException(maxDurationNanos, elapsedNanos);
        }
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

    public static final class ResponseDeadlineExceededException extends IOException {
        private final long limitNanos;
        private final long elapsedNanos;

        public ResponseDeadlineExceededException(long limitNanos, long elapsedNanos) {
            super("Provider response body deadline exceeded: limitNanos=" + limitNanos
                    + ", elapsedNanos=" + elapsedNanos);
            this.limitNanos = limitNanos;
            this.elapsedNanos = elapsedNanos;
        }

        public long limitNanos() {
            return limitNanos;
        }

        public long elapsedNanos() {
            return elapsedNanos;
        }
    }
}
