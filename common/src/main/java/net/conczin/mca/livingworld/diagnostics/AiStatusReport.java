package net.conczin.mca.livingworld.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Deterministic, operator-facing rendering for safe AI configuration and runtime diagnostics. */
public final class AiStatusReport {
    private AiStatusReport() {
    }

    public static List<String> format(AiDiagnosticsConfigSnapshot config, AiDiagnosticsSnapshot runtime) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(runtime, "runtime");

        List<String> lines = new ArrayList<>();
        lines.add("VillAIgence AI Status");
        appendStage(lines, "Chat", config.chat(), runtime.chat());
        appendStage(lines, "STT", config.stt(), runtime.stt());
        appendStage(lines, "TTS", config.tts(), runtime.tts());
        return List.copyOf(lines);
    }

    private static void appendStage(
            List<String> lines,
            String label,
            AiStageConfig config,
            AiOperationStatus runtime
    ) {
        StringBuilder summary = new StringBuilder(label)
                .append(": ")
                .append(config.state())
                .append(" | enabled=")
                .append(config.enabled())
                .append(" | credential=")
                .append(config.credentialConfigured());
        append(summary, "provider", config.provider());
        append(summary, "model", config.model());
        append(summary, "endpoint", config.endpointHost());
        append(summary, "format", config.format());
        lines.add(summary.toString());

        if (runtime.state() == AiOperationState.NEVER) {
            lines.add("  last: NEVER");
            return;
        }

        StringBuilder last = new StringBuilder("  last: ")
                .append(runtime.state())
                .append(" | ")
                .append(runtime.durationMillis())
                .append(" ms");
        append(last, "provider", runtime.provider());
        append(last, "model", runtime.model());
        append(last, "finish", runtime.finishReason());
        append(last, "type", runtime.errorType());
        append(last, "generation", runtime.generationId());
        append(last, "detail", runtime.detail());
        lines.add(last.toString());
    }

    private static void append(StringBuilder builder, String key, String value) {
        if (value == null || value.isBlank()) return;
        builder.append(" | ").append(key).append('=').append(value);
    }
}
