package net.conczin.mca.livingworld.voice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.ai.ProviderResponseLimits;
import net.conczin.mca.livingworld.audio.PcmAudio;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIAudioProviderResponseLimitTest {
    @Test
    void oversizedSttJsonIsRejectedBeforeJsonParsing() throws Exception {
        byte[] oversized = new byte[ProviderResponseLimits.STT_JSON_BYTES + 1];
        try (TestServer server = TestServer.fixedBody("/v1/audio/transcriptions", "application/json", oversized)) {
            LivingWorldConfig config = baseConfig();
            config.sttEndpoint = server.url("/v1/audio/transcriptions");
            config.sttApiKey = "stt-secret";

            Exception error = assertThrows(Exception.class, () ->
                    new OpenAIAudioProvider(config).transcribe(new PcmAudio(16_000, new short[]{1, 2})));

            assertTrue(error.getMessage().contains("byte limit"), error.toString());
            assertFalse(error.getMessage().contains("stt-secret"));
        }
    }

    @Test
    void declaredOversizedTtsBodyIsRejectedBeforeAnyBodyRead() throws Exception {
        AtomicInteger responseWrites = new AtomicInteger();
        try (TestServer server = TestServer.declaredOnly(
                "/v1/audio/speech",
                "audio/pcm;rate=24000;channels=1",
                (long) ProviderResponseLimits.TTS_AUDIO_BYTES + 1L,
                responseWrites
        )) {
            LivingWorldConfig config = baseConfig();
            config.ttsEndpoint = server.url("/v1/audio/speech");
            config.ttsApiKey = "tts-secret";
            config.ttsResponseFormat = "pcm";

            IOException error = assertThrows(IOException.class, () ->
                    new OpenAIAudioProvider(config).synthesize("test"));

            assertTrue(error.getMessage().contains("byte limit"), error.getMessage());
            assertFalse(error.getMessage().contains("tts-secret"));
            assertTrue(responseWrites.get() == 0, "oversized declared body should be rejected without body writes");
        }
    }

    private static LivingWorldConfig baseConfig() {
        LivingWorldConfig config = new LivingWorldConfig();
        config.allowInsecureLoopbackAiEndpoints = true;
        config.apiKey = "chat-key";
        config.connectTimeoutSeconds = 2;
        config.readTimeoutSeconds = 2;
        return config;
    }

    private static final class TestServer implements AutoCloseable {
        private final HttpServer server;

        private TestServer(HttpServer server) {
            this.server = server;
        }

        static TestServer fixedBody(String path, String contentType, byte[] body) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(path, exchange -> respondFixed(exchange, contentType, body));
            server.start();
            return new TestServer(server);
        }

        static TestServer declaredOnly(
                String path,
                String contentType,
                long declaredLength,
                AtomicInteger responseWrites
        ) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(path, exchange -> {
                try (exchange) {
                    exchange.getRequestBody().readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", contentType);
                    exchange.sendResponseHeaders(200, declaredLength);
                    responseWrites.set(0);
                }
            });
            server.start();
            return new TestServer(server);
        }

        String url(String path) {
            return "http://127.0.0.1:" + server.getAddress().getPort() + path;
        }

        private static void respondFixed(HttpExchange exchange, String contentType, byte[] body) throws IOException {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentType);
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
