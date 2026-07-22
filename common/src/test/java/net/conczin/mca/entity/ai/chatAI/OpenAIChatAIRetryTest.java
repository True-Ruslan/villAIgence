package net.conczin.mca.entity.ai.chatAI;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIChatAIRetryTest {
    @Test
    void retriesOneEmptyCompletionThenReturnsTheSecondUsableAnswer() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            int attempt = requests.incrementAndGet();
            String response = attempt == 1
                    ? """
                      {"id":"gen-empty","choices":[{"message":{"role":"assistant","content":null,"reasoning":"thinking"},"finish_reason":null}]}
                      """
                    : """
                      {"id":"gen-ok","choices":[{"message":{"role":"assistant","content":"{\\"message\\":\\"Привет!\\",\\"optionalCommand\\":\\"\\"}"},"finish_reason":"stop"}]}
                      """;
            respond(exchange, 200, response);
        });

        try {
            OpenAIChatAI.Answer answer = OpenAIChatAI.post(endpoint(server), "{}", "test-token");

            assertEquals(2, requests.get());
            assertNull(answer.error());
            assertNotNull(answer.answer());
            assertEquals("Привет!", answer.answer().message());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void lengthLimitedEmptyCompletionDoesNotRetryAndReturnsControlledFallback() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            requests.incrementAndGet();
            respond(exchange, 200, """
                    {"id":"gen-length","choices":[{"message":{"role":"assistant","content":null},"finish_reason":"length"}]}
                    """);
        });

        try {
            OpenAIChatAI.Answer answer = OpenAIChatAI.post(endpoint(server), "{}", "test-token");

            assertEquals(1, requests.get());
            assertNull(answer.answer());
            assertEquals("empty_response", answer.error());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void providerErrorWithNullContentIsPreservedWithoutRetry() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            requests.incrementAndGet();
            respond(exchange, 200, """
                    {
                      "id":"gen-error",
                      "choices":[{
                        "message":{"role":"assistant","content":null},
                        "finish_reason":"error",
                        "error":{"code":502,"message":"Provider disconnected","metadata":{"error_type":"provider_unavailable"}}
                      }]
                    }
                    """);
        });

        try {
            OpenAIChatAI.Answer answer = OpenAIChatAI.post(endpoint(server), "{}", "test-token");

            assertEquals(1, requests.get());
            assertNull(answer.answer());
            assertEquals("Provider disconnected", answer.error());
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat", exchange -> handler.handle(exchange));
        server.start();
        return server;
    }

    private static String endpoint(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/chat";
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        } finally {
            exchange.close();
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
