package net.conczin.mca.livingworld.diagnostics;

import net.conczin.mca.livingworld.ai.ChatCompletionResponseParser;

import java.util.Locale;

/** Safe bridge from OpenAI-compatible chat completion metadata into the diagnostics registry. */
public final class ChatDiagnosticsRecorder {
    private static final ThreadLocal<RequestState> REQUEST = ThreadLocal.withInitial(RequestState::empty);

    private ChatDiagnosticsRecorder() {
    }

    /** Starts an isolated logical chat request on the current worker thread. */
    public static void beginRequest() {
        REQUEST.set(RequestState.empty());
    }

    /**
     * Captures only non-content metadata from one provider attempt.
     * Raw assistant content and provider error messages are deliberately discarded here.
     */
    public static void captureCompletion(ChatCompletionResponseParser.ParsedCompletion completion) {
        if (completion == null) return;
        RequestState current = REQUEST.get();
        AttemptMetadata safe = new AttemptMetadata(
                AiOperationStatus.sanitize(completion.finishReason()),
                AiOperationStatus.sanitize(completion.errorType()),
                AiOperationStatus.sanitize(completion.generationId()),
                completion.reasoningPresent(),
                completion.content() != null && !completion.content().isBlank(),
                completion.error() != null && !completion.error().isBlank()
        );
        REQUEST.set(new RequestState(current.attempts() + 1, safe));
    }

    /** Finishes one logical request and clears all per-thread attempt metadata. */
    public static void finishRequest(
            String endpoint,
            String model,
            long durationMillis,
            boolean success
    ) {
        RequestState state = REQUEST.get();
        REQUEST.remove();
        AttemptMetadata latest = state.latest();
        int attempts = Math.max(1, state.attempts());

        if (success) {
            recordSafe(
                    AiOperationState.SUCCESS,
                    endpoint,
                    model,
                    durationMillis,
                    attempts,
                    latest,
                    "success"
            );
            return;
        }

        String detailCode;
        if (latest == null) {
            detailCode = "request_failed";
        } else if (latest.hadError()) {
            detailCode = "provider_error";
        } else if (!latest.hadContent()) {
            detailCode = "empty_response";
        } else {
            detailCode = "response_unusable";
        }
        recordSafe(
                AiOperationState.FAILURE,
                endpoint,
                model,
                durationMillis,
                attempts,
                latest,
                detailCode
        );
    }

    public static void recordSuccess(
            String endpoint,
            String model,
            long durationMillis,
            int attempts,
            ChatCompletionResponseParser.ParsedCompletion completion
    ) {
        recordSafe(
                AiOperationState.SUCCESS,
                endpoint,
                model,
                durationMillis,
                attempts,
                metadata(completion),
                "success"
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
        recordSafe(
                AiOperationState.FAILURE,
                endpoint,
                model,
                durationMillis,
                attempts,
                metadata(completion),
                detailCode
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

    private static AttemptMetadata metadata(ChatCompletionResponseParser.ParsedCompletion completion) {
        if (completion == null) return null;
        return new AttemptMetadata(
                AiOperationStatus.sanitize(completion.finishReason()),
                AiOperationStatus.sanitize(completion.errorType()),
                AiOperationStatus.sanitize(completion.generationId()),
                completion.reasoningPresent(),
                completion.content() != null && !completion.content().isBlank(),
                completion.error() != null && !completion.error().isBlank()
        );
    }

    private static void recordSafe(
            AiOperationState state,
            String endpoint,
            String model,
            long durationMillis,
            int attempts,
            AttemptMetadata metadata,
            String detailCode
    ) {
        String finishReason = metadata == null ? null : metadata.finishReason();
        String errorType = metadata == null ? null : metadata.errorType();
        String generationId = metadata == null ? null : metadata.generationId();
        String detail = detail(detailCode, attempts, metadata != null && metadata.reasoningPresent());
        if (state == AiOperationState.SUCCESS) {
            AiDiagnostics.recordSuccess(
                    AiOperation.CHAT,
                    durationMillis,
                    providerLabel(endpoint),
                    model,
                    finishReason,
                    errorType,
                    generationId,
                    detail
            );
        } else {
            AiDiagnostics.recordFailure(
                    AiOperation.CHAT,
                    durationMillis,
                    providerLabel(endpoint),
                    model,
                    finishReason,
                    errorType,
                    generationId,
                    detail
            );
        }
    }

    private static String detail(String code, int attempts, boolean reasoningPresent) {
        String safeCode = AiOperationStatus.sanitize(code);
        String prefix = safeCode.isBlank() ? "result" : safeCode;
        return prefix
                + "; attempts=" + Math.max(1, attempts)
                + "; reasoningPresent=" + reasoningPresent;
    }

    private record AttemptMetadata(
            String finishReason,
            String errorType,
            String generationId,
            boolean reasoningPresent,
            boolean hadContent,
            boolean hadError
    ) {
    }

    private record RequestState(int attempts, AttemptMetadata latest) {
        private static RequestState empty() {
            return new RequestState(0, null);
        }
    }
}
