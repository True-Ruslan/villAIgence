package net.conczin.mca.livingworld.diagnostics;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-local, lock-free registry for the latest logical result of each AI pipeline stage.
 *
 * <p>The registry intentionally stores only bounded operational metadata. It never accepts
 * credentials, prompts, transcripts, NPC answers, reasoning text, request bodies, or raw
 * provider payloads.</p>
 */
public final class AiDiagnostics {
    private static final AtomicReference<AiOperationStatus> CHAT = new AtomicReference<>(AiOperationStatus.never());
    private static final AtomicReference<AiOperationStatus> STT = new AtomicReference<>(AiOperationStatus.never());
    private static final AtomicReference<AiOperationStatus> TTS = new AtomicReference<>(AiOperationStatus.never());

    private AiDiagnostics() {
    }

    public static AiDiagnosticsSnapshot snapshot() {
        return new AiDiagnosticsSnapshot(CHAT.get(), STT.get(), TTS.get());
    }

    public static void recordSuccess(
            AiOperation operation,
            long durationMillis,
            String provider,
            String model,
            String finishReason,
            String errorType,
            String generationId,
            String detail
    ) {
        set(operation, new AiOperationStatus(
                AiOperationState.SUCCESS,
                System.currentTimeMillis(),
                durationMillis,
                provider,
                model,
                finishReason,
                errorType,
                generationId,
                detail
        ));
    }

    public static void recordFailure(
            AiOperation operation,
            long durationMillis,
            String provider,
            String model,
            String finishReason,
            String errorType,
            String generationId,
            String detail
    ) {
        set(operation, new AiOperationStatus(
                AiOperationState.FAILURE,
                System.currentTimeMillis(),
                durationMillis,
                provider,
                model,
                finishReason,
                errorType,
                generationId,
                detail
        ));
    }

    private static void set(AiOperation operation, AiOperationStatus status) {
        switch (Objects.requireNonNull(operation, "operation")) {
            case CHAT -> CHAT.set(status);
            case STT -> STT.set(status);
            case TTS -> TTS.set(status);
        }
    }

    static void resetForTests() {
        CHAT.set(AiOperationStatus.never());
        STT.set(AiOperationStatus.never());
        TTS.set(AiOperationStatus.never());
    }
}
