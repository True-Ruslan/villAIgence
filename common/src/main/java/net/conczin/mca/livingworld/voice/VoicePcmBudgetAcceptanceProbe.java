package net.conczin.mca.livingworld.voice;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Explicit operator probe for the exact voice-duration and server-wide PCM reservation bounds.
 *
 * <p>The probe allocates reservation counters only; it does not allocate PCM byte arrays and is never
 * invoked by Minecraft startup.</p>
 */
public final class VoicePcmBudgetAcceptanceProbe {
    private static final int DEFAULT_WORKERS = 256;
    private static final long DEFAULT_RESERVATION_BYTES = 1024L * 1024L;
    private static final long WAIT_SECONDS = 30L;

    private VoicePcmBudgetAcceptanceProbe() {
    }

    static Result run(long maxBytes, int workers, long reservationBytes) throws InterruptedException {
        if (maxBytes <= 0L) throw new IllegalArgumentException("maxBytes must be positive");
        if (workers <= 0 || workers > 4_096) {
            throw new IllegalArgumentException("workers must be between 1 and 4096");
        }
        if (reservationBytes <= 0L || reservationBytes > maxBytes) {
            throw new IllegalArgumentException("reservationBytes must be in 1..maxBytes");
        }

        int clampedLowSeconds = VoiceCaptureLimits.clampSeconds(Integer.MIN_VALUE);
        int clampedHighSeconds = VoiceCaptureLimits.clampSeconds(Integer.MAX_VALUE);
        VoicePcmBudget budget = new VoicePcmBudget(maxBytes);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch attempted = new CountDownLatch(workers);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicLong peakBytes = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        try {
            for (int i = 0; i < workers; i++) {
                executor.submit(() -> {
                    boolean reserved = false;
                    ready.countDown();
                    try {
                        start.await();
                        reserved = budget.tryReserve(reservationBytes);
                        if (reserved) {
                            accepted.incrementAndGet();
                            peakBytes.accumulateAndGet(budget.usedBytes(), Math::max);
                        } else {
                            rejected.incrementAndGet();
                        }
                    } catch (Throwable throwable) {
                        failure.compareAndSet(null, throwable);
                    } finally {
                        attempted.countDown();
                    }

                    if (reserved) {
                        try {
                            release.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            failure.compareAndSet(null, e);
                        } finally {
                            budget.release(reservationBytes);
                        }
                    }
                });
            }

            if (!ready.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("PCM probe workers did not become ready");
            }
            start.countDown();
            if (!attempted.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("PCM probe reservation phase timed out");
            }

            Throwable workerFailure = failure.get();
            if (workerFailure != null) {
                throw new IllegalStateException("PCM probe worker failed", workerFailure);
            }

            int acceptedCount = accepted.get();
            int rejectedCount = rejected.get();
            long observedPeak = Math.max(peakBytes.get(), budget.usedBytes());
            release.countDown();
            executor.shutdown();
            if (!executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("PCM probe workers did not terminate");
            }

            long finalBytes = budget.usedBytes();
            boolean recoveryReservationSucceeded = budget.tryReserve(maxBytes);
            if (recoveryReservationSucceeded) budget.release(maxBytes);

            long expectedAcceptedLong = Math.min((long) workers, maxBytes / reservationBytes);
            int expectedAccepted = Math.toIntExact(expectedAcceptedLong);
            boolean passed = clampedLowSeconds == VoiceCaptureLimits.MIN_SECONDS
                    && clampedHighSeconds == VoiceCaptureLimits.MAX_SECONDS
                    && acceptedCount == expectedAccepted
                    && rejectedCount == workers - expectedAccepted
                    && observedPeak == expectedAcceptedLong * reservationBytes
                    && observedPeak <= maxBytes
                    && finalBytes == 0L
                    && budget.usedBytes() == 0L
                    && recoveryReservationSucceeded;

            return new Result(
                    maxBytes,
                    workers,
                    reservationBytes,
                    clampedLowSeconds,
                    clampedHighSeconds,
                    acceptedCount,
                    rejectedCount,
                    observedPeak,
                    finalBytes,
                    recoveryReservationSucceeded,
                    passed
            );
        } finally {
            start.countDown();
            release.countDown();
            executor.shutdownNow();
        }
    }

    public static void main(String[] args) {
        try {
            int workers = args.length >= 1 ? parsePositiveInt(args[0], "workers") : DEFAULT_WORKERS;
            long reservationBytes = args.length >= 2
                    ? parsePositiveLong(args[1], "reservationBytes")
                    : DEFAULT_RESERVATION_BYTES;
            if (args.length > 2) {
                throw new IllegalArgumentException("Usage: VoicePcmBudgetAcceptanceProbe [workers] [reservationBytes]");
            }
            Result result = run(
                    VoiceCaptureLimits.MAX_ACTIVE_PCM_BYTES,
                    workers,
                    reservationBytes
            );
            System.out.println(result.toJson());
            if (!result.passed()) System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("PCM probe interrupted");
            System.exit(1);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("PCM probe failed: " + e.getMessage());
            System.exit(2);
        }
    }

    private static int parsePositiveInt(String value, String name) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) throw new NumberFormatException("non-positive");
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a positive integer", e);
        }
    }

    private static long parsePositiveLong(String value, String name) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0L) throw new NumberFormatException("non-positive");
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a positive integer", e);
        }
    }

    public record Result(
            long maxBytes,
            int workers,
            long reservationBytes,
            int clampedLowSeconds,
            int clampedHighSeconds,
            int accepted,
            int rejected,
            long peakBytes,
            long finalBytes,
            boolean recoveryReservationSucceeded,
            boolean passed
    ) {
        public String toJson() {
            String marker = passed
                    ? "VILLAIGENCE_PCM_PROBE_PASS"
                    : "VILLAIGENCE_PCM_PROBE_FAIL";
            return "{"
                    + "\"marker\":\"" + marker + "\","
                    + "\"maxBytes\":" + maxBytes + ","
                    + "\"workers\":" + workers + ","
                    + "\"reservationBytes\":" + reservationBytes + ","
                    + "\"clampedLowSeconds\":" + clampedLowSeconds + ","
                    + "\"clampedHighSeconds\":" + clampedHighSeconds + ","
                    + "\"accepted\":" + accepted + ","
                    + "\"rejected\":" + rejected + ","
                    + "\"peakBytes\":" + peakBytes + ","
                    + "\"finalBytes\":" + finalBytes + ","
                    + "\"recoveryReservationSucceeded\":" + recoveryReservationSucceeded + ","
                    + "\"passed\":" + passed
                    + "}";
        }
    }
}
