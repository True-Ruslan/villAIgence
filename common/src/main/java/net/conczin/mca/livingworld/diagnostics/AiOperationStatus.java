package net.conczin.mca.livingworld.diagnostics;

/** Immutable, content-safe observation for one completed AI pipeline operation. */
public record AiOperationStatus(
        AiOperationState state,
        long completedAtEpochMillis,
        long durationMillis,
        String provider,
        String model,
        String finishReason,
        String errorType,
        String generationId,
        String detail
) {
    static final int MAX_DIAGNOSTIC_CHARS = 160;

    public AiOperationStatus {
        state = state == null ? AiOperationState.NEVER : state;
        completedAtEpochMillis = Math.max(0L, completedAtEpochMillis);
        durationMillis = Math.max(0L, durationMillis);
        provider = sanitize(provider);
        model = sanitize(model);
        finishReason = sanitize(finishReason);
        errorType = sanitize(errorType);
        generationId = sanitize(generationId);
        detail = sanitize(detail);
    }

    static AiOperationStatus never() {
        return new AiOperationStatus(
                AiOperationState.NEVER,
                0L,
                0L,
                "",
                "",
                "",
                "",
                "",
                ""
        );
    }

    static String sanitize(String value) {
        if (value == null || value.isBlank()) return "";
        String cleaned = value.trim().replace('\n', ' ').replace('\r', ' ');
        return cleaned.length() <= MAX_DIAGNOSTIC_CHARS
                ? cleaned
                : cleaned.substring(0, MAX_DIAGNOSTIC_CHARS);
    }
}
