package net.conczin.mca.livingworld.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIAudioProviderTest {
    @Test
    void jsonTranscriptionBodyUsesRawBase64WavAndLanguageHint() {
        String body = OpenAIAudioProvider.createJsonTranscriptionBody(
                new byte[]{0, 1, 2},
                "openai/gpt-4o-mini-transcribe",
                "ru"
        );

        assertTrue(body.contains("\"model\":\"openai/gpt-4o-mini-transcribe\""));
        assertTrue(body.contains("\"input_audio\""));
        assertTrue(body.contains("\"data\":\"AAEC\""));
        assertTrue(body.contains("\"format\":\"wav\""));
        assertTrue(body.contains("\"language\":\"ru\""));
        assertFalse(body.contains("data:audio"));
    }

    @Test
    void jsonTranscriptionBodyOmitsBlankLanguage() {
        String body = OpenAIAudioProvider.createJsonTranscriptionBody(
                new byte[]{0},
                "openai/whisper-large-v3",
                "  "
        );

        assertFalse(body.contains("\"language\""));
    }
}
