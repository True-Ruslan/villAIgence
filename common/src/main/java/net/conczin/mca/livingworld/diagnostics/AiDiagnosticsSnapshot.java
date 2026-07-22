package net.conczin.mca.livingworld.diagnostics;

/** Immutable snapshot of the latest process-local diagnostics for the AI pipeline. */
public record AiDiagnosticsSnapshot(
        AiOperationStatus chat,
        AiOperationStatus stt,
        AiOperationStatus tts
) {
    public AiDiagnosticsSnapshot {
        chat = chat == null ? AiOperationStatus.never() : chat;
        stt = stt == null ? AiOperationStatus.never() : stt;
        tts = tts == null ? AiOperationStatus.never() : tts;
    }
}
