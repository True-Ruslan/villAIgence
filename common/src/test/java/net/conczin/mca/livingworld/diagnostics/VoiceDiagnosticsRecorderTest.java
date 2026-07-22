package net.conczin.mca.livingworld.diagnostics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceDiagnosticsRecorderTest {
    @BeforeEach
    void reset() {
        AiDiagnostics.resetForTests();
    }

    @Test
    void recordsSttSuccessWithoutSpeechContent() {
        VoiceDiagnosticsRecorder.recordSuccess(
                AiOperation.STT,
                "https://openrouter.ai/api/v1/audio/transcriptions",
                "transcribe-model",
                "json_base64",
                321L
        );

        AiOperationStatus status = AiDiagnostics.snapshot().stt();
        assertEquals(AiOperationState.SUCCESS, status.state());
        assertEquals("openrouter", status.provider());
        assertEquals("transcribe-model", status.model());
        assertEquals(321L, status.durationMillis());
        assertEquals("format=json_base64", status.detail());
    }

    @Test
    void classifiesProviderFailureWithoutStoringRawErrorText() {
        VoiceDiagnosticsRecorder.recordFailure(
                AiOperation.TTS,
                "https://openrouter.ai/api/v1/audio/speech",
                "tts-model",
                "pcm",
                88L,
                new IOException("AI audio text-to-speech failed (HTTP 402): SECRET_PROVIDER_RESPONSE")
        );

        AiOperationStatus status = AiDiagnostics.snapshot().tts();
        assertEquals(AiOperationState.FAILURE, status.state());
        assertEquals("http_402", status.errorType());
        assertTrue(status.detail().contains("format=pcm"));
        assertFalse(status.toString().contains("SECRET_PROVIDER_RESPONSE"));
    }

    @Test
    void classifiesTimeoutAndRateLimitDeterministically() {
        VoiceDiagnosticsRecorder.recordFailure(
                AiOperation.STT,
                "https://api.openai.com/v1/audio/transcriptions",
                "stt-model",
                "multipart",
                1000L,
                new SocketTimeoutException("SECRET_TIMEOUT_DETAIL")
        );
        assertEquals("timeout", AiDiagnostics.snapshot().stt().errorType());

        VoiceDiagnosticsRecorder.recordFailure(
                AiOperation.TTS,
                "https://api.openai.com/v1/audio/speech",
                "tts-model",
                "wav",
                90L,
                new IOException("provider failed HTTP 429 SECRET_RATE_LIMIT_BODY")
        );
        assertEquals("http_429", AiDiagnostics.snapshot().tts().errorType());
        assertFalse(AiDiagnostics.snapshot().tts().toString().contains("SECRET_RATE_LIMIT_BODY"));
    }

    @Test
    void rejectsChatOperationBecauseChatHasDedicatedRecorder() {
        assertThrows(IllegalArgumentException.class, () -> VoiceDiagnosticsRecorder.recordSuccess(
                AiOperation.CHAT,
                "https://api.openai.com/v1/chat/completions",
                "model",
                "",
                1L
        ));
    }
}
