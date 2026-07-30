package net.conczin.mca.livingworld.voice;

import com.sun.net.httpserver.HttpServer;
import net.conczin.mca.livingworld.LivingWorldConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIAudioProviderRedirectTest {
    @Test
    void authenticatedAudioRequestDoesNotFollowRedirectToAnotherOrigin() throws Exception {
        AtomicInteger destinationRequests = new AtomicInteger();
        AtomicReference<String> destinationAuthorization = new AtomicReference<>();

        try (RedirectTarget target = RedirectTarget.start(destinationRequests, destinationAuthorization);
             RedirectSource source = RedirectSource.start(target.url())) {
            LivingWorldConfig config = new LivingWorldConfig();
            config.allowInsecureLoopbackAiEndpoints = true;
            config.ttsEndpoint = source.url();
            config.ttsApiKey = "redirect-secret";
            config.ttsModel = "test-tts-model";
            config.ttsVoice = "alloy";
            config.ttsResponseFormat = "pcm";
            config.ttsPcmSampleRate = 24_000;

            IOException error = assertThrows(IOException.class, () ->
                    new OpenAIAudioProvider(config).synthesize("redirect test"));

            assertTrue(error.getMessage().contains("HTTP 302"), error.getMessage());
            assertEquals(0, destinationRequests.get());
            assertEquals(null, destinationAuthorization.get());
        }
    }

    private static final class RedirectSource implements AutoCloseable {
        private final HttpServer server;

        private RedirectSource(HttpServer server) {
            this.server = server;
        }

        static RedirectSource start(String destination) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/audio/speech", exchange -> {
                try (exchange) {
                    exchange.getRequestBody().readAllBytes();
                    exchange.getResponseHeaders().set("Location", destination);
                    exchange.sendResponseHeaders(302, -1);
                }
            });
            server.start();
            return new RedirectSource(server);
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/audio/speech";
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class RedirectTarget implements AutoCloseable {
        private final HttpServer server;

        private RedirectTarget(HttpServer server) {
            this.server = server;
        }

        static RedirectTarget start(
                AtomicInteger requests,
                AtomicReference<String> authorization
        ) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/destination", exchange -> {
                try (exchange) {
                    requests.incrementAndGet();
                    authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    exchange.getRequestBody().readAllBytes();
                    byte[] body = new byte[]{1, 0};
                    exchange.getResponseHeaders().set("Content-Type", "audio/pcm;rate=24000;channels=1");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
            });
            server.start();
            return new RedirectTarget(server);
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/destination";
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
