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

    @Test
    void richTtsModelUsesPersistentVoiceMoodInstructionsAndSpeed() {
        TtsRequest request = new TtsRequest(
                "Привет",
                "cedar",
                new TtsVoiceStyle("Speak warmly and calmly.", 1.08)
        );

        String body = OpenAIAudioProvider.createSpeechBody(request, "gpt-4o-mini-tts");

        assertTrue(body.contains("\"model\":\"gpt-4o-mini-tts\""));
        assertTrue(body.contains("\"voice\":\"cedar\""));
        assertTrue(body.contains("\"input\":\"Привет\""));
        assertTrue(body.contains("\"instructions\":\"Speak warmly and calmly.\""));
        assertTrue(body.contains("\"speed\":1.08"));
        assertTrue(body.contains("\"response_format\":\"wav\""));
    }

    @Test
    void legacyTtsModelsOmitUnsupportedInstructionsButKeepVoiceAndSpeed() {
        TtsRequest request = new TtsRequest(
                "Привет",
                "marin",
                new TtsVoiceStyle("Speak sadly.", 0.92)
        );

        String body = OpenAIAudioProvider.createSpeechBody(request, "tts-1");

        assertTrue(body.contains("\"voice\":\"marin\""));
        assertTrue(body.contains("\"speed\":0.92"));
        assertFalse(body.contains("\"instructions\""));
    }
}
