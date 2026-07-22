package net.conczin.mca.livingworld.admission;

import net.conczin.mca.livingworld.diagnostics.AiOperation;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-local, non-blocking admission controller for external AI operations.
 *
 * <p>No method waits for capacity. Overload is rejected immediately so Minecraft threads cannot
 * become blocked behind provider work.</p>
 */
public final class AiAdmissionController {
    private static final StageState CHAT = new StageState();
    private static final StageState STT = new StageState();
    private static final StageState TTS = new StageState();

    private AiAdmissionController() {
    }

    public static AiAdmissionResult tryAcquire(
            AiOperation operation,
            UUID actorId,
            AiAdmissionSettings settings
    ) {
        return tryAcquire(operation, actorId, settings, System.nanoTime());
    }

    static AiAdmissionResult tryAcquire(
            AiOperation operation,
            UUID actorId,
            AiAdmissionSettings settings,
            long nowNanos
    ) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(settings, "settings");

        StageState state = state(operation);
        if (state.providerCooldownUntilNanos.get() > nowNanos) {
            state.rejected.incrementAndGet();
            return new AiAdmissionResult(AiAdmissionDecision.PROVIDER_COOLDOWN, null);
        }

        int maxConcurrent = Math.max(1, settings.maxConcurrent(operation));
        if (!tryIncrementActive(state.active, maxConcurrent)) {
            state.rejected.incrementAndGet();
            return new AiAdmissionResult(AiAdmissionDecision.SATURATED, null);
        }

        long cooldownNanos = millisToNanos(settings.perPlayerCooldownMillis());
        if (cooldownNanos > 0L && !acceptPlayerCooldown(state, actorId, nowNanos, cooldownNanos)) {
            release(state);
            state.rejected.incrementAndGet();
            return new AiAdmissionResult(AiAdmissionDecision.PLAYER_COOLDOWN, null);
        }

        return new AiAdmissionResult(AiAdmissionDecision.ALLOWED, new Permit(state));
    }

    public static void onRateLimited(AiOperation operation, long cooldownMillis) {
        onRateLimited(operation, cooldownMillis, System.nanoTime());
    }

    static void onRateLimited(AiOperation operation, long cooldownMillis, long nowNanos) {
        Objects.requireNonNull(operation, "operation");
        long durationNanos = millisToNanos(cooldownMillis);
        if (durationNanos <= 0L) return;

        long candidate = saturatingAdd(nowNanos, durationNanos);
        AtomicLong deadline = state(operation).providerCooldownUntilNanos;
        deadline.accumulateAndGet(candidate, Math::max);
    }

    public static AiAdmissionSnapshot snapshot(AiAdmissionSettings settings) {
        return snapshot(settings, System.nanoTime());
    }

    static AiAdmissionSnapshot snapshot(AiAdmissionSettings settings, long nowNanos) {
        Objects.requireNonNull(settings, "settings");
        return new AiAdmissionSnapshot(
                snapshotStage(CHAT, settings.chatMaxConcurrentRequests(), nowNanos),
                snapshotStage(STT, settings.sttMaxConcurrentRequests(), nowNanos),
                snapshotStage(TTS, settings.ttsMaxConcurrentRequests(), nowNanos)
        );
    }

    private static AiAdmissionStageSnapshot snapshotStage(StageState state, int configuredMax, long nowNanos) {
        long remainingNanos = Math.max(0L, state.providerCooldownUntilNanos.get() - nowNanos);
        return new AiAdmissionStageSnapshot(
                Math.max(0, state.active.get()),
                Math.max(1, configuredMax),
                Math.max(0L, state.rejected.get()),
                TimeUnit.NANOSECONDS.toMillis(remainingNanos)
        );
    }

    private static boolean tryIncrementActive(AtomicInteger active, int maxConcurrent) {
        while (true) {
            int current = active.get();
            if (current >= maxConcurrent) return false;
            if (active.compareAndSet(current, current + 1)) return true;
        }
    }

    private static boolean acceptPlayerCooldown(
            StageState state,
            UUID actorId,
            long nowNanos,
            long cooldownNanos
    ) {
        AtomicLong lastAccepted = state.lastAcceptedByActor.computeIfAbsent(actorId, ignored -> new AtomicLong(Long.MIN_VALUE));
        while (true) {
            long previous = lastAccepted.get();
            if (previous != Long.MIN_VALUE) {
                long elapsed = nowNanos - previous;
                if (elapsed >= 0L && elapsed < cooldownNanos) return false;
            }
            if (lastAccepted.compareAndSet(previous, nowNanos)) return true;
        }
    }

    private static StageState state(AiOperation operation) {
        return switch (operation) {
            case CHAT -> CHAT;
            case STT -> STT;
            case TTS -> TTS;
        };
    }

    private static void release(StageState state) {
        state.active.updateAndGet(current -> Math.max(0, current - 1));
    }

    private static long millisToNanos(long millis) {
        if (millis <= 0L) return 0L;
        long maxMillis = Long.MAX_VALUE / 1_000_000L;
        return Math.min(millis, maxMillis) * 1_000_000L;
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    static void resetForTests() {
        CHAT.reset();
        STT.reset();
        TTS.reset();
    }

    public static final class Permit implements AutoCloseable {
        private final StageState state;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Permit(StageState state) {
            this.state = state;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) release(state);
        }
    }

    private static final class StageState {
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicLong rejected = new AtomicLong();
        private final AtomicLong providerCooldownUntilNanos = new AtomicLong();
        private final ConcurrentHashMap<UUID, AtomicLong> lastAcceptedByActor = new ConcurrentHashMap<>();

        private void reset() {
            active.set(0);
            rejected.set(0L);
            providerCooldownUntilNanos.set(0L);
            lastAcceptedByActor.clear();
        }
    }
}
