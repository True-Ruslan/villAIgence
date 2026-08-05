package net.conczin.mca.livingworld.ai;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * One monotonic time budget shared by every network phase and retry of an AI request.
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

    public static AiRequestDeadline start(int connectTimeoutMillis, int readTimeoutMillis) {
        return start(connectTimeoutMillis, readTimeoutMillis, System::nanoTime);
    }

    static AiRequestDeadline start(
            int connectTimeoutMillis,
            int readTimeoutMillis,
            LongSupplier nanoTime
    ) {
        Objects.requireNonNull(nanoTime, "nanoTime");
        long budgetMillis = (long) requirePositive(connectTimeoutMillis, "connectTimeoutMillis")
                + requirePositive(readTimeoutMillis, "readTimeoutMillis");
        long budgetNanos = budgetMillis * NANOS_PER_MILLISECOND;
        return new AiRequestDeadline(nanoTime, nanoTime.getAsLong(), budgetNanos);
    }

    /**
     * Returns the configured timeout bounded by the remaining shared budget.
     *
     * @throws DeadlineExceededException when the shared deadline is already exhausted
     */
    public int boundedTimeoutMillis(int configuredTimeoutMillis) throws DeadlineExceededException {
        int configured = requirePositive(configuredTimeoutMillis, "configuredTimeoutMillis");
        long remainingNanos = remainingNanos();
        if (remainingNanos <= 0L) {
            throw new DeadlineExceededException();
        }

        long remainingMillis = ceilDivide(remainingNanos, NANOS_PER_MILLISECOND);
        return (int) Math.max(1L, Math.min((long) configured, remainingMillis));
    }

    public boolean isExpired() {
        return remainingNanos() <= 0L;
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
