package net.conczin.mca.livingworld.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountVerificationAcceptanceProbeTest {
    @Test
    void acceptsOnlyLiteralLoopbackUrisWithoutUserInfoOrFragment() {
        assertEquals(
                URI.create("http://127.0.0.1:18080/v1/mca/verify/success"),
                AccountVerificationAcceptanceProbe.validateLoopbackUri(
                        "http://127.0.0.1:18080/v1/mca/verify/success"
                )
        );
        assertEquals(
                URI.create("https://[::1]:18443/v1/mca/verify/success"),
                AccountVerificationAcceptanceProbe.validateLoopbackUri(
                        "https://[::1]:18443/v1/mca/verify/success"
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                AccountVerificationAcceptanceProbe.validateLoopbackUri(
                        "http://localhost:18080/v1/mca/verify/success"
                ));
        assertThrows(IllegalArgumentException.class, () ->
                AccountVerificationAcceptanceProbe.validateLoopbackUri(
                        "http://192.168.1.10:18080/v1/mca/verify/success"
                ));
        assertThrows(IllegalArgumentException.class, () ->
                AccountVerificationAcceptanceProbe.validateLoopbackUri(
                        "http://user:password@127.0.0.1:18080/v1/mca/verify/success"
                ));
        assertThrows(IllegalArgumentException.class, () ->
                AccountVerificationAcceptanceProbe.validateLoopbackUri(
                        "http://127.0.0.1:18080/v1/mca/verify/success#fragment"
                ));
    }

    @Test
    void executesTheProductionBoundedTransportForSuccess() throws Exception {
        try (TestServer server = TestServer.create()) {
            server.server.createContext("/success", exchange ->
                    respondFixed(exchange, 200, "{\"answer\":\"success\"}"));
            server.start();

            assertEquals(
                    AccountVerificationClient.Result.SUCCESS,
                    AccountVerificationClient.execute(server.uri("/success"), 1_000, 1_000)
            );
        }
    }

    @Test
    void verificationRedirectIsNotFollowed() throws Exception {
        AtomicInteger targetHits = new AtomicInteger();
        try (TestServer server = TestServer.create()) {
            server.server.createContext("/redirect", exchange -> {
                try (exchange) {
                    exchange.getResponseHeaders().set("Location", server.uri("/target").toString());
                    exchange.sendResponseHeaders(307, -1);
                }
            });
            server.server.createContext("/target", exchange -> {
                targetHits.incrementAndGet();
                respondFixed(exchange, 200, "{\"answer\":\"success\"}");
            });
            server.start();

            assertEquals(
                    AccountVerificationClient.Result.ERROR,
                    AccountVerificationClient.execute(server.uri("/redirect"), 1_000, 1_000)
            );
            assertEquals(0, targetHits.get());
        }
    }

    @Test
    void verificationDeclaredOversizeFailsBeforePayloadUse() throws Exception {
        try (TestServer server = TestServer.create()) {
            server.server.createContext("/oversize", exchange -> {
                try (exchange) {
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(
                            200,
                            (long) ProviderResponseLimits.VERIFICATION_JSON_BYTES + 1L
                    );
                }
            });
            server.start();

            assertEquals(
                    AccountVerificationClient.Result.ERROR,
                    AccountVerificationClient.execute(server.uri("/oversize"), 1_000, 1_000)
            );
        }
    }

    private static void respondFixed(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try (exchange) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private static final class TestServer implements AutoCloseable {
        private final HttpServer server;

        private TestServer(HttpServer server) {
            this.server = server;
        }

        static TestServer create() throws IOException {
            return new TestServer(HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0));
        }

        void start() {
            server.start();
        }

        URI uri(String path) {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
