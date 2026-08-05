package net.conczin.mca.livingworld.ai;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * One monotonic budget shared by one or more AI network stages.
 *
 * <p>The deadline is intentionally independent from wall-clock time. Timeout values are rounded up
 * to the nearest millisecond so an operation never receives a zero timeout, which
 * {@link java.net.URLConnection} would interpret as infinite.</p>
 */
public final class AiRequestDeadline {
    private static final long NANOS_PER_MILLISECOND = TimeUnit.MILLISECONDS.toNanos(1L);

    private final LongSupplier nanoTime;
    private final long startedNanos;
    private final long budgetNanos;

    private AiRequestDeadline(LongSupplier nanoTime, long startedNanos, long budgetNanos) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.startedNanos = startedNanos;
        this.budgetNanos = budgetNanos;
    }

    /** Creates one deadline for a single provider request, including every retry. */
    public static AiRequestDeadline start(int connectTimeoutMillis, int readTimeoutMillis) {
        return start(connectTimeoutMillis, readTimeoutMillis, System::nanoTime);
    }

    static AiRequestDeadline start(
            int connectTimeoutMillis,
            int readTimeoutMillis,
            LongSupplier nanoTime
    ) {
        long budgetMillis = (long) requirePositive(connectTimeoutMillis, "connectTimeoutMillis")
                + requirePositive(readTimeoutMillis, "readTimeoutMillis");
        return startTotalMillis(budgetMillis, nanoTime);
    }

    /** Creates one total deadline that can span STT, Chat retries and TTS. */
    public static AiRequestDeadline startTotalMillis(int totalTimeoutMillis) {
        return startTotalMillis(totalTimeoutMillis, System::nanoTime);
    }

    static AiRequestDeadline startTotalMillis(int totalTimeoutMillis, LongSupplier nanoTime) {
        return startTotalMillis((long) requirePositive(totalTimeoutMillis, "totalTimeoutMillis"), nanoTime);
    }

    private static AiRequestDeadline startTotalMillis(long totalTimeoutMillis, LongSupplier nanoTime) {
        Objects.requireNonNull(nanoTime, "nanoTime");
        long budgetNanos = Math.multiplyExact(totalTimeoutMillis, NANOS_PER_MILLISECOND);
        return new AiRequestDeadline(nanoTime, nanoTime.getAsLong(), budgetNanos);
    }

    /**
     * Returns the configured timeout bounded by the remaining shared budget.
     *
     * @throws DeadlineExceededException when the shared deadline is already exhausted
     */
    public int boundedTimeoutMillis(int configuredTimeoutMillis) throws DeadlineExceededException {
        int configured = requirePositive(configuredTimeoutMillis, "configuredTimeoutMillis");
        long remainingNanos = remainingNanosOrThrow();
        long remainingMillis = ceilDivide(remainingNanos, NANOS_PER_MILLISECOND);
        return (int) Math.max(1L, Math.min((long) configured, remainingMillis));
    }

    /** Fails before entering another stage when no shared budget remains. */
    public void throwIfExpired() throws DeadlineExceededException {
        remainingNanosOrThrow();
    }

    public boolean isExpired() {
        return remainingNanos() <= 0L;
    }

    long remainingNanosOrThrow() throws DeadlineExceededException {
        long remainingNanos = remainingNanos();
        if (remainingNanos <= 0L) {
            throw new DeadlineExceededException();
        }
        return remainingNanos;
    }

    private long remainingNanos() {
        long elapsedNanos = nanoTime.getAsLong() - startedNanos;
        return budgetNanos - elapsedNanos;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
        return value;
    }

    private static long ceilDivide(long dividend, long divisor) {
        return 1L + ((dividend - 1L) / divisor);
    }

    public static final class DeadlineExceededException extends IOException {
        public DeadlineExceededException() {
            super("AI provider request deadline exceeded");
        }
    }
}
