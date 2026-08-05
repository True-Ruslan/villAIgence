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
    private final long deadlineNanos;

    private AiRequestDeadline(LongSupplier nanoTime, long deadlineNanos) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.deadlineNanos = deadlineNanos;
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
        long budgetMillis = saturatingAdd(
                requirePositive(connectTimeoutMillis, "connectTimeoutMillis"),
                requirePositive(readTimeoutMillis, "readTimeoutMillis")
        );
        long budgetNanos = saturatingMultiply(budgetMillis, NANOS_PER_MILLISECOND);
        long startedNanos = nanoTime.getAsLong();
        return new AiRequestDeadline(nanoTime, saturatingAdd(startedNanos, budgetNanos));
    }

    /**
     * Returns the configured timeout bounded by the remaining shared budget.
     *
     * @throws DeadlineExceededException when the shared deadline is already exhausted
     */
    public int boundedTimeoutMillis(int configuredTimeoutMillis) throws DeadlineExceededException {
        int configured = requirePositive(configuredTimeoutMillis, "configuredTimeoutMillis");
        long remainingNanos = deadlineNanos - nanoTime.getAsLong();
        if (remainingNanos <= 0L) {
            throw new DeadlineExceededException();
        }

        long remainingMillis = ceilDivide(remainingNanos, NANOS_PER_MILLISECOND);
        long bounded = Math.min((long) configured, remainingMillis);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, bounded));
    }

    public boolean isExpired() {
        return deadlineNanos - nanoTime.getAsLong() <= 0L;
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

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0L && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
    }

    private static long saturatingMultiply(long left, long right) {
        if (left == 0L || right == 0L) {
            return 0L;
        }
        if (left > 0L && right > 0L && left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    public static final class DeadlineExceededException extends IOException {
        public DeadlineExceededException() {
            super("AI provider request deadline exceeded");
        }
    }
}
