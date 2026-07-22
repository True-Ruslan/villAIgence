package net.conczin.mca.livingworld.voice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.audio.PcmAudio;
import net.conczin.mca.livingworld.audio.WavCodec;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

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
    void richTtsModelUsesPersistentVoiceMoodInstructionsAndResolvedFormat() {
        TtsRequest request = new TtsRequest(
                "Привет",
                "cedar",
                new TtsVoiceStyle("Speak warmly and calmly.", 1.08)
        );

        String body = OpenAIAudioProvider.createSpeechBody(request, "gpt-4o-mini-tts", TtsResponseFormat.PCM);

        assertTrue(body.contains("\"model\":\"gpt-4o-mini-tts\""));
        assertTrue(body.contains("\"voice\":\"cedar\""));
        assertTrue(body.contains("\"input\":\"Привет\""));
        assertTrue(body.contains("\"instructions\":\"Speak warmly and calmly.\""));
        assertTrue(body.contains("\"speed\":1.08"));
        assertTrue(body.contains("\"response_format\":\"pcm\""));
    }

    @Test
    void legacyTtsModelsOmitUnsupportedInstructionsButKeepVoiceAndSpeed() {
        TtsRequest request = new TtsRequest(
                "Привет",
                "marin",
                new TtsVoiceStyle("Speak sadly.", 0.92)
        );

        String body = OpenAIAudioProvider.createSpeechBody(request, "tts-1", TtsResponseFormat.WAV);

        assertTrue(body.contains("\"voice\":\"marin\""));
        assertTrue(body.contains("\"speed\":0.92"));
        assertTrue(body.contains("\"response_format\":\"wav\""));
        assertFalse(body.contains("\"instructions\""));
    }

    @Test
    void pcmHttpResponseUsesMimeRateAndDecodesLittleEndianMono() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        try (TestServer server = TestServer.start(
                "audio/pcm;rate=24000;channels=1",
                new byte[]{1, 0, (byte) 0xff, (byte) 0xff},
                "gen-pcm-1",
                requestBody
        )) {
            LivingWorldConfig config = pcmConfig(server.url());
            PcmAudio audio = new OpenAIAudioProvider(config).synthesize(
                    new TtsRequest("Привет", "rex", TtsVoiceStyle.NEUTRAL)
            );

            assertEquals(24_000, audio.sampleRate());
            assertArrayEquals(new short[]{1, -1}, audio.samples());
            assertTrue(requestBody.get().contains("\"response_format\":\"pcm\""));
            assertTrue(requestBody.get().contains("\"voice\":\"rex\""));
            assertEquals(48_000, audio.resampleTo(48_000).sampleRate());
            assertEquals(4, audio.resampleTo(48_000).samples().length);
        }
    }

    @Test
    void pcmMissingRateUsesConfiguredFallbackAndMissingChannelsAssumesMono() throws Exception {
        try (TestServer server = TestServer.start("audio/pcm", new byte[]{2, 0}, "gen-fallback", null)) {
            LivingWorldConfig config = pcmConfig(server.url());
            config.ttsPcmSampleRate = 16_000;

            PcmAudio audio = new OpenAIAudioProvider(config).synthesize(
                    new TtsRequest("test", "rex", TtsVoiceStyle.NEUTRAL)
            );

            assertEquals(16_000, audio.sampleRate());
            assertArrayEquals(new short[]{2}, audio.samples());
        }
    }

    @Test
    void pcmRejectsStereoMalformedRateOddBodyAndWrongContentType() throws Exception {
        assertPcmFailure("audio/pcm;rate=24000;channels=2", new byte[]{0, 0}, "channels");
        assertPcmFailure("audio/pcm;rate=oops;channels=1", new byte[]{0, 0}, "rate");
        assertPcmFailure("audio/pcm;rate=24000;channels=1", new byte[]{0}, "PCM16");
        assertPcmFailure("audio/mpeg", new byte[]{0, 0}, "Content-Type");
    }

    @Test
    void ttsDecodeFailureIncludesGenerationIdWithoutRawPayload() throws Exception {
        try (TestServer server = TestServer.start("audio/mpeg", new byte[]{1, 2, 3, 4}, "gen-safe-42", null)) {
            LivingWorldConfig config = pcmConfig(server.url());

            IOException error = assertThrows(IOException.class, () -> new OpenAIAudioProvider(config).synthesize(
                    new TtsRequest("test", "rex", TtsVoiceStyle.NEUTRAL)
            ));

            assertTrue(error.getMessage().contains("gen-safe-42"));
            assertFalse(error.getMessage().contains("AQIDBA"));
        }
    }

    @Test
    void wavResponseBehaviorRemainsOperational() throws Exception {
        byte[] wav = WavCodec.encodePcm16Mono(new short[]{10, -10}, 24_000);
        AtomicReference<String> requestBody = new AtomicReference<>();
        try (TestServer server = TestServer.start("audio/wav", wav, "gen-wav", requestBody)) {
            LivingWorldConfig config = pcmConfig(server.url());
            config.ttsResponseFormat = "wav";

            PcmAudio audio = new OpenAIAudioProvider(config).synthesize(
                    new TtsRequest("test", "marin", TtsVoiceStyle.NEUTRAL)
            );

            assertEquals(24_000, audio.sampleRate());
            assertArrayEquals(new short[]{10, -10}, audio.samples());
            assertTrue(requestBody.get().contains("\"response_format\":\"wav\""));
        }
    }

    private static void assertPcmFailure(String contentType, byte[] body, String expectedMessagePart) throws Exception {
        try (TestServer server = TestServer.start(contentType, body, "gen-failure", null)) {
            LivingWorldConfig config = pcmConfig(server.url());
            IOException error = assertThrows(IOException.class, () -> new OpenAIAudioProvider(config).synthesize(
                    new TtsRequest("test", "rex", TtsVoiceStyle.NEUTRAL)
            ));
            assertTrue(error.getMessage().contains(expectedMessagePart), error.getMessage());
        }
    }

    private static LivingWorldConfig pcmConfig(String endpoint) {
        LivingWorldConfig config = new LivingWorldConfig();
        config.ttsEndpoint = endpoint;
        config.ttsApiKey = "test-key";
        config.ttsModel = "test-tts-model";
        config.ttsVoice = "rex";
        config.ttsResponseFormat = "pcm";
        config.ttsPcmSampleRate = 24_000;
        return config;
    }

    private static final class TestServer implements AutoCloseable {
        private final HttpServer server;

        private TestServer(HttpServer server) {
            this.server = server;
        }

        static TestServer start(
                String contentType,
                byte[] responseBody,
                String generationId,
                AtomicReference<String> requestBodyCapture
        ) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/audio/speech", exchange -> respond(
                    exchange, contentType, responseBody, generationId, requestBodyCapture
            ));
            server.start();
            return new TestServer(server);
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/audio/speech";
        }

        private static void respond(
                HttpExchange exchange,
                String contentType,
                byte[] responseBody,
                String generationId,
                AtomicReference<String> requestBodyCapture
        ) throws IOException {
            try (exchange) {
                if (requestBodyCapture != null) {
                    requestBodyCapture.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                } else {
                    exchange.getRequestBody().readAllBytes();
                }
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("X-Generation-Id", generationId);
                exchange.sendResponseHeaders(200, responseBody.length);
                exchange.getResponseBody().write(responseBody);
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
