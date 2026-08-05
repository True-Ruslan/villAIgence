package net.conczin.mca.livingworld.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the real Chat HTTP transport against a deterministic loopback provider. */
class ChatCompletionHttpClientIntegrationTest {
    @Test
    void retriesOneEmptyCompletionAndReturnsOneUsableAnswer() throws Exception {
        try (ScriptedChatServer server = ScriptedChatServer.retryThenSuccess()) {
            ChatCompletionHttpClient.Result result = ChatCompletionHttpClient.post(
                    server.endpoint(),
                    "{\"model\":\"test-model\",\"messages\":[]}",
                    "test-secret",
                    500,
                    2_000,
                    ChatCompletionHttpClient.AttemptObserver.NOOP
            );

            assertNull(result.error());
            assertNotNull(result.completion());
            assertEquals("Привет из детерминированного провайдера", result.completion().content());
            assertEquals(2, result.attempts());
            assertEquals(2, server.requestCount());
            assertEquals(List.of("Bearer test-secret", "Bearer test-secret"), server.authorizationHeaders());
            assertTrue(server.lastRequestBody().contains("\"model\":\"test-model\""));
        }
    }

    @Test
    void retrySharesOneEndToEndBudgetInsteadOfReceivingTwoFullReadTimeouts() throws Exception {
        try (ScriptedChatServer server = ScriptedChatServer.slowEmptyThenBlocked()) {
            AtomicReference<ChatCompletionHttpClient.Result> result = new AtomicReference<>();
            long started = System.nanoTime();

            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> result.set(ChatCompletionHttpClient.post(
                    server.endpoint(),
                    "{\"model\":\"deadline-model\",\"messages\":[]}",
                    "deadline-secret",
                    100,
                    2_000,
                    ChatCompletionHttpClient.AttemptObserver.NOOP
            )));

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertNotNull(result.get());
            assertEquals(ChatCompletionHttpClient.REQUEST_DEADLINE_ERROR, result.get().error());
            assertEquals(2, server.requestCount());
            assertTrue(
                    elapsedMillis < 3_000,
                    "A retry must share one connect+read budget; elapsed=" + elapsedMillis + "ms"
            );
        }
    }

    private static final class ScriptedChatServer implements AutoCloseable {
        private static final String EMPTY_COMPLETION = """
                {"id":"empty-1","choices":[{"message":{"content":null},"finish_reason":"stop"}]}
                """;
        private static final String SUCCESS_COMPLETION = """
                {"id":"success-2","choices":[{"message":{"content":"Привет из детерминированного провайдера"},"finish_reason":"stop"}]}
                """;

        private final HttpServer server;
        private final ExecutorService executor;
        private final AtomicInteger requestCount = new AtomicInteger();
        private final List<String> authorizationHeaders = new CopyOnWriteArrayList<>();
        private final AtomicReference<String> lastRequestBody = new AtomicReference<>("");
        private final Mode mode;
        private final CountDownLatch blockedRequest = new CountDownLatch(1);

        private ScriptedChatServer(HttpServer server, ExecutorService executor, Mode mode) {
            this.server = server;
            this.executor = executor;
            this.mode = mode;
        }

        static ScriptedChatServer retryThenSuccess() throws IOException {
            return start(Mode.RETRY_THEN_SUCCESS);
        }

        static ScriptedChatServer slowEmptyThenBlocked() throws IOException {
            return start(Mode.SLOW_EMPTY_THEN_BLOCKED);
        }

        private static ScriptedChatServer start(Mode mode) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "villaigence-mock-chat-provider");
                thread.setDaemon(true);
                return thread;
            });
            ScriptedChatServer fixture = new ScriptedChatServer(server, executor, mode);
            server.setExecutor(executor);
            server.createContext("/v1/chat/completions", fixture::handle);
            server.start();
            return fixture;
        }

        ProviderEndpoint endpoint() {
            return ProviderEndpointPolicy.parse(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions",
                    true
            );
        }

        int requestCount() {
            return requestCount.get();
        }

        List<String> authorizationHeaders() {
            return List.copyOf(authorizationHeaders);
        }

        String lastRequestBody() {
            return lastRequestBody.get();
        }

        private void handle(HttpExchange exchange) throws IOException {
            int requestNumber = requestCount.incrementAndGet();
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            if (mode == Mode.RETRY_THEN_SUCCESS) {
                respondJson(exchange, requestNumber == 1 ? EMPTY_COMPLETION : SUCCESS_COMPLETION);
                return;
            }

            if (requestNumber == 1) {
                try {
                    Thread.sleep(1_500L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                    return;
                }
                respondJson(exchange, EMPTY_COMPLETION);
                return;
            }

            try (exchange) {
                try {
                    blockedRequest.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private static void respondJson(HttpExchange exchange, String json) throws IOException {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        }

        @Override
        public void close() {
            blockedRequest.countDown();
            server.stop(0);
            executor.shutdownNow();
        }

        private enum Mode {
            RETRY_THEN_SUCCESS,
            SLOW_EMPTY_THEN_BLOCKED
        }
    }
}
