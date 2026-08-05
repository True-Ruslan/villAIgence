package net.conczin.mca.livingworld.voice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.audio.PcmAudio;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the production STT and TTS HTTP clients against one deterministic loopback provider.
 * Physical microphone capture and Voice Chat playback remain separate installed-client canaries.
 */
class OpenAIProviderEndToEndIntegrationTest {
    @Test
    void pcmTravelsThroughRealSttAndTtsHttpPathsWithoutExternalNetwork() throws Exception {
        try (MockAudioProvider server = MockAudioProvider.start()) {
            LivingWorldConfig config = config(server);
            OpenAIAudioProvider provider = new OpenAIAudioProvider(config);

            String transcript = provider.transcribe(new PcmAudio(
                    16_000,
                    new short[]{0, 1200, -1200, 300, -300}
            ));
            PcmAudio speech = provider.synthesize(new TtsRequest(
                    transcript,
                    "rex",
                    TtsVoiceStyle.NEUTRAL
            ));

            assertEquals("Привет, кузнец", transcript);
            assertEquals(24_000, speech.sampleRate());
            assertArrayEquals(new short[]{1, -1, 0x1234}, speech.samples());

            assertEquals(1, server.sttRequests.get());
            assertEquals(1, server.ttsRequests.get());
            assertEquals("Bearer stt-secret", server.sttAuthorization.get());
            assertEquals("Bearer tts-secret", server.ttsAuthorization.get());

            String sttContentType = server.sttContentType.get();
            assertTrue(sttContentType.startsWith("multipart/form-data; boundary="), sttContentType);
            byte[] sttBody = server.sttBody.get();
            assertTrue(containsAscii(sttBody, "name=\"model\""));
            assertTrue(containsAscii(sttBody, "phase-c-stt"));
            assertTrue(containsAscii(sttBody, "name=\"language\""));
            assertTrue(containsAscii(sttBody, "ru"));
            assertTrue(containsAscii(sttBody, "filename=\"speech.wav\""));
            assertTrue(indexOf(sttBody, new byte[]{'R', 'I', 'F', 'F'}) >= 0);

            String ttsBody = new String(server.ttsBody.get(), StandardCharsets.UTF_8);
            assertTrue(ttsBody.contains("\"model\":\"phase-c-tts\""), ttsBody);
            assertTrue(ttsBody.contains("\"voice\":\"rex\""), ttsBody);
            assertTrue(ttsBody.contains("\"input\":\"Привет, кузнец\""), ttsBody);
            assertTrue(ttsBody.contains("\"response_format\":\"pcm\""), ttsBody);
        }
    }

    private static LivingWorldConfig config(MockAudioProvider server) {
        LivingWorldConfig config = new LivingWorldConfig();
        config.provider = "openai";
        config.allowInsecureLoopbackAiEndpoints = true;
        config.endpoint = server.url("/v1/chat/completions");
        config.apiKey = "chat-secret";

        config.sttEndpoint = server.url("/v1/audio/transcriptions");
        config.sttApiKey = "stt-secret";
        config.sttModel = "phase-c-stt";
        config.sttLanguage = "ru";
        config.sttRequestFormat = "multipart";

        config.ttsEndpoint = server.url("/v1/audio/speech");
        config.ttsApiKey = "tts-secret";
        config.ttsModel = "phase-c-tts";
        config.ttsVoice = "rex";
        config.ttsResponseFormat = "pcm";
        config.ttsPcmSampleRate = 24_000;
        config.connectTimeoutSeconds = 2;
        config.readTimeoutSeconds = 2;
        return config;
    }

    private static boolean containsAscii(byte[] haystack, String needle) {
        return indexOf(haystack, needle.getBytes(StandardCharsets.UTF_8)) >= 0;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        if (needle.length == 0) return 0;
        for (int offset = 0; offset <= haystack.length - needle.length; offset++) {
            boolean matches = true;
            for (int index = 0; index < needle.length; index++) {
                if (haystack[offset + index] != needle[index]) {
                    matches = false;
                    break;
                }
            }
            if (matches) return offset;
        }
        return -1;
    }

    private static final class MockAudioProvider implements AutoCloseable {
        private static final byte[] PCM_RESPONSE = new byte[]{1, 0, (byte) 0xff, (byte) 0xff, 0x34, 0x12};

        private final HttpServer server;
        private final AtomicInteger sttRequests = new AtomicInteger();
        private final AtomicInteger ttsRequests = new AtomicInteger();
        private final AtomicReference<String> sttAuthorization = new AtomicReference<>();
        private final AtomicReference<String> ttsAuthorization = new AtomicReference<>();
        private final AtomicReference<String> sttContentType = new AtomicReference<>();
        private final AtomicReference<byte[]> sttBody = new AtomicReference<>();
        private final AtomicReference<byte[]> ttsBody = new AtomicReference<>();

        private MockAudioProvider(HttpServer server) {
            this.server = server;
        }

        static MockAudioProvider start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            MockAudioProvider fixture = new MockAudioProvider(server);
            server.createContext("/v1/audio/transcriptions", fixture::transcribe);
            server.createContext("/v1/audio/speech", fixture::synthesize);
            server.start();
            return fixture;
        }

        String url(String path) {
            return "http://127.0.0.1:" + server.getAddress().getPort() + path;
        }

        private void transcribe(HttpExchange exchange) throws IOException {
            sttRequests.incrementAndGet();
            sttAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sttContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            sttBody.set(exchange.getRequestBody().readAllBytes());
            respond(
                    exchange,
                    "application/json",
                    "{\"text\":\"Привет, кузнец\"}".getBytes(StandardCharsets.UTF_8),
                    "phase-c-stt-1"
            );
        }

        private void synthesize(HttpExchange exchange) throws IOException {
            ttsRequests.incrementAndGet();
            ttsAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            ttsBody.set(exchange.getRequestBody().readAllBytes());
            respond(
                    exchange,
                    "audio/pcm;rate=24000;channels=1",
                    PCM_RESPONSE,
                    "phase-c-tts-1"
            );
        }

        private static void respond(
                HttpExchange exchange,
                String contentType,
                byte[] body,
                String generationId
        ) throws IOException {
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("X-Generation-Id", generationId);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
