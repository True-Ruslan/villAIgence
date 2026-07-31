package net.conczin.mca.livingworld.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SttRequestFormatTest {
    @Test
    void parsesExplicitFormatsAndFallsBackToAuto() {
        assertEquals(SttRequestFormat.MULTIPART, SttRequestFormat.parse("multipart"));
        assertEquals(SttRequestFormat.JSON_BASE64, SttRequestFormat.parse("json_base64"));
        assertEquals(SttRequestFormat.AUTO, SttRequestFormat.parse("unknown"));
        assertEquals(SttRequestFormat.AUTO, SttRequestFormat.parse(null));
    }

    @Test
    void autoUsesJsonBase64ForOpenRouter() {
        assertEquals(
                SttRequestFormat.JSON_BASE64,
                SttRequestFormat.AUTO.resolve("https://openrouter.ai/api/v1/audio/transcriptions")
        );
        assertEquals(
                SttRequestFormat.JSON_BASE64,
                SttRequestFormat.AUTO.resolve("https://api.openrouter.ai/api/v1/audio/transcriptions")
        );
    }

    @Test
    void autoKeepsMultipartForOpenAiCompatibleFileUploadEndpoints() {
        assertEquals(
                SttRequestFormat.MULTIPART,
                SttRequestFormat.AUTO.resolve("https://api.openai.com/v1/audio/transcriptions")
        );
    }

    @Test
    void malformedAndLookalikeEndpointsAreNeverClassifiedBySubstring() {
        assertFalse(SttRequestFormat.isOpenRouterEndpoint("not-a-uri-openrouter.ai"));
        assertFalse(SttRequestFormat.isOpenRouterEndpoint(
                "https://evil.example/proxy/openrouter.ai/api/v1/audio/transcriptions"
        ));
        assertFalse(SttRequestFormat.isOpenRouterEndpoint(
                "https://openrouter.ai.example.invalid/api/v1/audio/transcriptions"
        ));
    }

    @Test
    void explicitFormatOverridesEndpointDetection() {
        assertEquals(
                SttRequestFormat.MULTIPART,
                SttRequestFormat.MULTIPART.resolve("https://openrouter.ai/api/v1/audio/transcriptions")
        );
        assertEquals(
                SttRequestFormat.JSON_BASE64,
                SttRequestFormat.JSON_BASE64.resolve("https://api.openai.com/v1/audio/transcriptions")
        );
    }
}
