package net.conczin.mca.livingworld.diagnostics;

import net.conczin.mca.livingworld.ai.ChatCompletionResponseParser;

import java.util.Locale;

/** Safe bridge from OpenAI-compatible chat completion metadata into the diagnostics registry. */
public final class ChatDiagnosticsRecorder {
    private ChatDiagnosticsRecorder() {
    }

    public static void recordSuccess(
            String endpoint,
            String model,
            long durationMillis,
            int attempts,
            ChatCompletionResponseParser.ParsedCompletion completion
    ) {
        AiDiagnostics.recordSuccess(
                AiOperation.CHAT,
                durationMillis,
                providerLabel(endpoint),
                model,
                completion == null ? null : completion.finishReason(),
                completion == null ? null : completion.errorType(),
                completion == null ? null : completion.generationId(),
                detail("success", attempts, completion)
        );
    }

    public static void recordFailure(
            String endpoint,
            String model,
            long durationMillis,
            int attempts,
            ChatCompletionResponseParser.ParsedCompletion completion,
            String detailCode
    ) {
        AiDiagnostics.recordFailure(
                AiOperation.CHAT,
                durationMillis,
                providerLabel(endpoint),
                model,
                completion == null ? null : completion.finishReason(),
                completion == null ? null : completion.errorType(),
                completion == null ? null : completion.generationId(),
                detail(detailCode, attempts, completion)
        );
    }

    static String providerLabel(String endpoint) {
        String host = AiDiagnosticsConfigSnapshot.endpointHost(endpoint);
        if ("<invalid>".equals(host)) return "unknown";
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.equals("openrouter.ai") || normalized.endsWith(".openrouter.ai")) return "openrouter";
        if (normalized.equals("api.openai.com") || normalized.endsWith(".api.openai.com")) return "openai";
        return host;
    }

    static long elapsedMillis(long startedNanos) {
        if (startedNanos <= 0L) return 0L;
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private static String detail(
            String code,
            int attempts,
            ChatCompletionResponseParser.ParsedCompletion completion
    ) {
        String safeCode = AiOperationStatus.sanitize(code);
        String prefix = safeCode.isBlank() ? "result" : safeCode;
        return prefix
                + "; attempts=" + Math.max(1, attempts)
                + "; reasoningPresent=" + (completion != null && completion.reasoningPresent());
    }
}
