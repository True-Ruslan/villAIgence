package net.conczin.mca.livingworld.diagnostics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiDiagnosticsTest {
    @BeforeEach
    void reset() {
        AiDiagnostics.resetForTests();
    }

    @Test
    void startsWithNeverForEveryOperation() {
        AiDiagnosticsSnapshot snapshot = AiDiagnostics.snapshot();

        assertEquals(AiOperationState.NEVER, snapshot.chat().state());
        assertEquals(AiOperationState.NEVER, snapshot.stt().state());
        assertEquals(AiOperationState.NEVER, snapshot.tts().state());
    }

    @Test
    void successReplacesPreviousStateWithSafeMetadata() {
        AiDiagnostics.recordFailure(
                AiOperation.CHAT,
                12L,
                "openrouter",
                "model-a",
                null,
                "provider_error",
                "gen-old",
                "temporary failure"
        );

        AiDiagnostics.recordSuccess(
                AiOperation.CHAT,
                34L,
                "openrouter",
                "model-b",
                "stop",
                null,
                "gen-new",
                "attempts=2"
        );

        AiOperationStatus status = AiDiagnostics.snapshot().chat();
        assertEquals(AiOperationState.SUCCESS, status.state());
        assertEquals(34L, status.durationMillis());
        assertEquals("openrouter", status.provider());
        assertEquals("model-b", status.model());
        assertEquals("stop", status.finishReason());
        assertEquals("gen-new", status.generationId());
        assertEquals("attempts=2", status.detail());
        assertTrue(status.completedAtEpochMillis() > 0L);
    }

    @Test
    void failureMetadataIsSingleLineAndBounded() {
        String multiline = "line1\nline2\r" + "x".repeat(400);

        AiDiagnostics.recordFailure(
                AiOperation.STT,
                -10L,
                "  openrouter  ",
                "  transcribe-model  ",
                null,
                "  http_402  ",
                "  generation-1  ",
                multiline
        );

        AiOperationStatus status = AiDiagnostics.snapshot().stt();
        assertEquals(AiOperationState.FAILURE, status.state());
        assertEquals(0L, status.durationMillis());
        assertEquals("openrouter", status.provider());
        assertEquals("transcribe-model", status.model());
        assertEquals("http_402", status.errorType());
        assertEquals("generation-1", status.generationId());
        assertFalse(status.detail().contains("\n"));
        assertFalse(status.detail().contains("\r"));
        assertTrue(status.detail().length() <= 160);
    }

    @Test
    void operationsAreIndependent() {
        AiDiagnostics.recordSuccess(
                AiOperation.TTS,
                21L,
                "openai",
                "tts-1",
                null,
                null,
                null,
                "format=wav"
        );

        AiDiagnosticsSnapshot snapshot = AiDiagnostics.snapshot();
        assertEquals(AiOperationState.NEVER, snapshot.chat().state());
        assertEquals(AiOperationState.NEVER, snapshot.stt().state());
        assertEquals(AiOperationState.SUCCESS, snapshot.tts().state());
    }
}
