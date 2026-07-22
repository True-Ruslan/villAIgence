package net.conczin.mca.livingworld.diagnostics;

import net.conczin.mca.livingworld.ai.ChatCompletionResponseParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatDiagnosticsRecorderTest {
    @BeforeEach
    void reset() {
        AiDiagnostics.resetForTests();
        ChatDiagnosticsRecorder.beginRequest();
    }

    @Test
    void recordsSuccessfulFinalCompletionWithoutContentOrReasoningText() {
        ChatCompletionResponseParser.ParsedCompletion completion = new ChatCompletionResponseParser.ParsedCompletion(
                "VISIBLE_NPC_CONTENT_MUST_NOT_BE_STORED",
                null,
                null,
                "stop",
                "gen-123",
                true
        );

        ChatDiagnosticsRecorder.recordSuccess(
                "https://openrouter.ai/api/v1/chat/completions",
                "openai/gpt-4.1-mini",
                812L,
                2,
                completion
        );

        AiOperationStatus status = AiDiagnostics.snapshot().chat();
        assertEquals(AiOperationState.SUCCESS, status.state());
        assertEquals("openrouter", status.provider());
        assertEquals("openai/gpt-4.1-mini", status.model());
        assertEquals("stop", status.finishReason());
        assertEquals("gen-123", status.generationId());
        assertTrue(status.detail().contains("attempts=2"));
        assertTrue(status.detail().contains("reasoningPresent=true"));
        assertFalse(status.toString().contains("VISIBLE_NPC_CONTENT_MUST_NOT_BE_STORED"));
    }

    @Test
    void recordsProviderFailureWithoutRawProviderErrorMessage() {
        ChatCompletionResponseParser.ParsedCompletion completion = new ChatCompletionResponseParser.ParsedCompletion(
                null,
                "RAW_PROVIDER_ERROR_WITH_POSSIBLE_SENSITIVE_ECHO",
                "rate_limit",
                null,
                "gen-error",
                false
        );

        ChatDiagnosticsRecorder.recordFailure(
                "https://api.openai.com/v1/chat/completions",
                "gpt-4.1-mini",
                155L,
                1,
                completion,
                "provider_error"
        );

        AiOperationStatus status = AiDiagnostics.snapshot().chat();
        assertEquals(AiOperationState.FAILURE, status.state());
        assertEquals("openai", status.provider());
        assertEquals("rate_limit", status.errorType());
        assertEquals("gen-error", status.generationId());
        assertTrue(status.detail().contains("provider_error"));
        assertTrue(status.detail().contains("attempts=1"));
        assertFalse(status.toString().contains("RAW_PROVIDER_ERROR_WITH_POSSIBLE_SENSITIVE_ECHO"));
    }

    @Test
    void endpointClassificationFallsBackToHostWithoutEchoingPath() {
        ChatDiagnosticsRecorder.recordFailure(
                "https://llm.example.test/private/SECRET_PATH",
                "custom-model",
                10L,
                1,
                null,
                "transport_error"
        );

        AiOperationStatus status = AiDiagnostics.snapshot().chat();
        assertEquals("llm.example.test", status.provider());
        assertFalse(status.toString().contains("SECRET_PATH"));
    }

    @Test
    void requestLifecycleKeepsOnlySafeMetadataFromLatestRetryAttempt() {
        ChatDiagnosticsRecorder.beginRequest();
        ChatDiagnosticsRecorder.captureCompletion(new ChatCompletionResponseParser.ParsedCompletion(
                null,
                null,
                null,
                "stop",
                "gen-first",
                true
        ));
        ChatDiagnosticsRecorder.captureCompletion(new ChatCompletionResponseParser.ParsedCompletion(
                "SECOND_ATTEMPT_CONTENT_MUST_NOT_BE_STORED",
                null,
                null,
                "stop",
                "gen-second",
                false
        ));

        ChatDiagnosticsRecorder.finishRequest(
                "https://openrouter.ai/api/v1/chat/completions",
                "model",
                42L,
                true
        );

        AiOperationStatus status = AiDiagnostics.snapshot().chat();
        assertEquals(AiOperationState.SUCCESS, status.state());
        assertEquals("gen-second", status.generationId());
        assertTrue(status.detail().contains("attempts=2"));
        assertTrue(status.detail().contains("reasoningPresent=false"));
        assertFalse(status.toString().contains("SECOND_ATTEMPT_CONTENT_MUST_NOT_BE_STORED"));
    }

    @Test
    void requestLifecycleClearsCapturedMetadataAfterFinish() {
        ChatDiagnosticsRecorder.beginRequest();
        ChatDiagnosticsRecorder.captureCompletion(new ChatCompletionResponseParser.ParsedCompletion(
                "content",
                null,
                null,
                "stop",
                "old-generation",
                false
        ));
        ChatDiagnosticsRecorder.finishRequest("https://api.openai.com/v1/chat/completions", "model", 10L, true);

        ChatDiagnosticsRecorder.beginRequest();
        ChatDiagnosticsRecorder.finishRequest("https://api.openai.com/v1/chat/completions", "model", 11L, false);

        AiOperationStatus status = AiDiagnostics.snapshot().chat();
        assertEquals(AiOperationState.FAILURE, status.state());
        assertEquals("", status.generationId());
        assertTrue(status.detail().contains("request_failed"));
    }
}
