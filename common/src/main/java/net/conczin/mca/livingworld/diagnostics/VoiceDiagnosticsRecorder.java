package net.conczin.mca.livingworld.diagnostics;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Locale;

/** Content-safe diagnostics recorder for speech-to-text and text-to-speech operations. */
public final class VoiceDiagnosticsRecorder {
    private VoiceDiagnosticsRecorder() {
    }

    public static void recordSuccess(
            AiOperation operation,
            String endpoint,
            String model,
            String format,
            long durationMillis
    ) {
        validateVoiceOperation(operation);
        AiDiagnostics.recordSuccess(
                operation,
                durationMillis,
                ChatDiagnosticsRecorder.providerLabel(endpoint),
                model,
                null,
                null,
                null,
                detail(format)
        );
    }

    public static void recordFailure(
            AiOperation operation,
            String endpoint,
            String model,
            String format,
            long durationMillis,
            Throwable error
    ) {
        validateVoiceOperation(operation);
        AiDiagnostics.recordFailure(
                operation,
                durationMillis,
                ChatDiagnosticsRecorder.providerLabel(endpoint),
                model,
                null,
                classify(error),
                null,
                detail(format)
        );
    }

    public static long elapsedMillis(long startedNanos) {
        if (startedNanos <= 0L) return 0L;
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    static String classify(Throwable error) {
        if (containsType(error, SocketTimeoutException.class)) return "timeout";
        String message = combinedMessages(error).toUpperCase(Locale.ROOT);
        if (message.contains("HTTP 402")) return "http_402";
        if (message.contains("HTTP 429")) return "http_429";
        if (containsType(error, IOException.class)) return "io_error";
        return "runtime_error";
    }

    private static void validateVoiceOperation(AiOperation operation) {
        if (operation != AiOperation.STT && operation != AiOperation.TTS) {
            throw new IllegalArgumentException("Voice diagnostics support only STT and TTS operations");
        }
    }

    private static String detail(String format) {
        String safeFormat = AiOperationStatus.sanitize(format);
        return safeFormat.isBlank() ? "" : "format=" + safeFormat;
    }

    private static boolean containsType(Throwable error, Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    private static String combinedMessages(Throwable error) {
        StringBuilder result = new StringBuilder();
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null) {
                if (!result.isEmpty()) result.append(' ');
                result.append(current.getMessage());
            }
            current = current.getCause();
        }
        return result.toString();
    }
}
