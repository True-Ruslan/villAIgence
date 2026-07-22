package net.conczin.mca.livingworld.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TtsResponseFormatTest {
    @Test
    void autoUsesPcmForOpenRouterAndWavElsewhere() {
        assertEquals(TtsResponseFormat.PCM,
                TtsResponseFormat.AUTO.resolve("https://openrouter.ai/api/v1/audio/speech"));
        assertEquals(TtsResponseFormat.PCM,
                TtsResponseFormat.AUTO.resolve("https://api.openrouter.ai/v1/audio/speech"));
        assertEquals(TtsResponseFormat.WAV,
                TtsResponseFormat.AUTO.resolve("https://api.openai.com/v1/audio/speech"));
        assertEquals(TtsResponseFormat.WAV,
                TtsResponseFormat.AUTO.resolve("https://example.invalid/v1/audio/speech"));
    }

    @Test
    void explicitFormatOverridesEndpointDetection() {
        assertEquals(TtsResponseFormat.WAV,
                TtsResponseFormat.WAV.resolve("https://openrouter.ai/api/v1/audio/speech"));
        assertEquals(TtsResponseFormat.PCM,
                TtsResponseFormat.PCM.resolve("https://api.openai.com/v1/audio/speech"));
    }

    @Test
    void parserNormalizesUnknownValuesToAuto() {
        assertEquals(TtsResponseFormat.AUTO, TtsResponseFormat.parse(null));
        assertEquals(TtsResponseFormat.AUTO, TtsResponseFormat.parse(""));
        assertEquals(TtsResponseFormat.AUTO, TtsResponseFormat.parse("unknown"));
        assertEquals(TtsResponseFormat.AUTO, TtsResponseFormat.parse(" AUTO "));
        assertEquals(TtsResponseFormat.WAV, TtsResponseFormat.parse(" wav "));
        assertEquals(TtsResponseFormat.PCM, TtsResponseFormat.parse("PCM"));
    }

    @Test
    void endpointDetectionUsesHostnameNotPathSubstring() {
        assertEquals(TtsResponseFormat.WAV,
                TtsResponseFormat.AUTO.resolve("https://evil.example/proxy/openrouter.ai/api/v1/audio/speech"));
    }
}
