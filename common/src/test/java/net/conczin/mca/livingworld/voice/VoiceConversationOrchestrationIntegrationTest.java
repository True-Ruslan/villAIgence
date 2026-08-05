package net.conczin.mca.livingworld.voice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.ai.AiRequestDeadline;
import net.conczin.mca.livingworld.ai.ChatCompletionHttpClient;
import net.conczin.mca.livingworld.ai.ProviderEndpointPolicy;
import net.conczin.mca.livingworld.ai.StructuredAiResponseParser;
import net.conczin.mca.livingworld.audio.PcmAudio;
import net.conczin.mca.livingworld.memory2.Memory2DialogueLifecycle;
import net.conczin.mca.livingworld.memory2.Memory2RelationshipChangeIngestor;
import net.conczin.mca.livingworld.memory2.MemoryEvent;
import net.conczin.mca.livingworld.memory2.MemoryEventStore;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipChange;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that one production deadline can span STT, Chat retries and TTS while gameplay commits
 * remain outside provider-attempt loops.
 */
class VoiceConversationOrchestrationIntegrationTest {
    @TempDir
    Path worldRoot;

    @Test
    void sharedDeadlineAllowsOneCommittedTurnAcrossChatRetry() throws Exception {
        try (ScriptedVoiceProvider server = ScriptedVoiceProvider.success()) {
            LivingWorldConfig config = config(server);
            OpenAIAudioProvider audio = new OpenAIAudioProvider(config);
            AiRequestDeadline deadline = AiRequestDeadline.startTotalMillis(3_000);
            UUID npcId = UUID.randomUUID();
            UUID playerId = UUID.randomUUID();

            String transcript = audio.transcribe(inputAudio(), deadline);
            ChatCompletionHttpClient.Result chat = ChatCompletionHttpClient.post(
                    ProviderEndpointPolicy.parse(server.url("/v1/chat/completions"), true),
                    "{\"model\":\"phase-c-orchestration\",\"messages\":[]}",
                    "chat-secret",
                    500,
                    2_000,
                    deadline,
                    ChatCompletionHttpClient.AttemptObserver.NOOP
            );

            assertEquals(null, chat.error());
            assertEquals(2, chat.attempts());
            StructuredAiResponseParser.ParsedResponse parsed =
                    StructuredAiResponseParser.parse(chat.completion().content());
            assertEquals("Идём к кузнице", parsed.message());

            commitSuccessfulChat(npcId, playerId, transcript, parsed, 100L);
            PcmAudio speech = audio.synthesize(
                    new TtsRequest(parsed.message(), "cedar", TtsVoiceStyle.NEUTRAL),
                    deadline
            );

            assertArrayEquals(new short[]{1, -1, 0x1234}, speech.samples());
            assertEquals(1, server.sttRequests.get());
            assertEquals(2, server.chatRequests.get());
            assertEquals(1, server.ttsRequests.get());
            assertCommittedExactlyOnce(npcId, playerId);
        }
    }

    @Test
    void ttsCannotExtendPastOneConversationBudgetAndDoesNotDuplicateCommittedChat() throws Exception {
        try (ScriptedVoiceProvider server = ScriptedVoiceProvider.blockedTts()) {
            LivingWorldConfig config = config(server);
            OpenAIAudioProvider audio = new OpenAIAudioProvider(config);
            AiRequestDeadline deadline = AiRequestDeadline.startTotalMillis(1_600);
            UUID npcId = UUID.randomUUID();
            UUID playerId = UUID.randomUUID();

            long startedNanos = System.nanoTime();
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                String transcript = audio.transcribe(inputAudio(), deadline);
                ChatCompletionHttpClient.Result chat = ChatCompletionHttpClient.post(
                        ProviderEndpointPolicy.parse(server.url("/v1/chat/completions"), true),
                        "{\"model\":\"phase-c-orchestration\",\"messages\":[]}",
                        "chat-secret",
                        500,
                        2_000,
                        deadline,
                        ChatCompletionHttpClient.AttemptObserver.NOOP
                );
                assertEquals(null, chat.error());
                StructuredAiResponseParser.ParsedResponse parsed =
                        StructuredAiResponseParser.parse(chat.completion().content());
                commitSuccessfulChat(npcId, playerId, transcript, parsed, 200L);

                IOException failure = assertThrows(IOException.class, () -> audio.synthesize(
                        new TtsRequest(parsed.message(), "cedar", TtsVoiceStyle.NEUTRAL),
                        deadline
                ));
                assertTrue(
                        failure.getMessage().contains("deadline exceeded"),
                        "Expected explicit shared-deadline failure but got: " + failure.getMessage()
                );
            });

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
            assertTrue(elapsedMillis < 2_500, "TTS received a fresh timeout budget: " + elapsedMillis + "ms");
            assertEquals(1, server.sttRequests.get());
            assertEquals(2, server.chatRequests.get());
            assertEquals(1, server.ttsRequests.get());
            assertCommittedExactlyOnce(npcId, playerId);
        }
    }

    private void commitSuccessfulChat(
            UUID npcId,
            UUID playerId,
            String transcript,
            StructuredAiResponseParser.ParsedResponse parsed,
            long gameTime
    ) {
        Memory2DialogueLifecycle.recordSuccessful(
                true,
                worldRoot,
                npcId,
                playerId,
                gameTime,
                transcript,
                Optional.ofNullable(parsed.message()),
                32,
                1_000L + gameTime
        );

        LivingWorldRelationshipChange change = LivingWorldRelationshipStore.forWorld(worldRoot)
                .applyDeltaWithResult(npcId, playerId, parsed.relationshipDelta(), 2);
        Memory2RelationshipChangeIngestor.recordIfEnabled(
                true,
                worldRoot,
                npcId,
                playerId,
                gameTime,
                change,
                32,
                2_000L + gameTime
        );
    }

    private void assertCommittedExactlyOnce(UUID npcId, UUID playerId) {
        List<MemoryEvent> events = MemoryEventStore.forWorld(worldRoot).getRecent(npcId, 32);
        assertEquals(1L, events.stream().filter(event -> event.type() == MemoryEvent.Type.DIALOGUE).count());
        assertEquals(1L, events.stream().filter(event -> event.type() == MemoryEvent.Type.RELATIONSHIP_CHANGE).count());
        assertEquals(
                new LivingWorldRelationshipState(1, 0, 0, 1),
                LivingWorldRelationshipStore.forWorld(worldRoot).get(npcId, playerId)
        );
    }

    private static PcmAudio inputAudio() {
        return new PcmAudio(16_000, new short[]{0, 1200, -1200, 300, -300});
    }

    private static LivingWorldConfig config(ScriptedVoiceProvider server) {
        LivingWorldConfig config = new LivingWorldConfig();
        config.provider = "openai";
        config.allowInsecureLoopbackAiEndpoints = true;
        config.endpoint = server.url("/v1/chat/completions");
        config.apiKey = "chat-secret";
        config.sttEndpoint = server.url("/v1/audio/transcriptions");
        config.sttApiKey = "stt-secret";
        config.sttModel = "phase-c-stt";
        config.sttRequestFormat = "multipart";
        config.ttsEndpoint = server.url("/v1/audio/speech");
        config.ttsApiKey = "tts-secret";
        config.ttsModel = "phase-c-tts";
        config.ttsResponseFormat = "pcm";
        config.ttsPcmSampleRate = 24_000;
        config.connectTimeoutSeconds = 2;
        config.readTimeoutSeconds = 4;
        return config;
    }

    private static final class ScriptedVoiceProvider implements AutoCloseable {
        private static final String EMPTY_CHAT = """
                {"id":"empty","choices":[{"message":{"content":null},"finish_reason":"stop"}]}
                """;
        private static final String SUCCESS_CHAT = """
                {"id":"success","choices":[{"message":{"content":"{\\"message\\":\\"Идём к кузнице\\",\\"optionalCommand\\":\\"\\",\\"relationshipDelta\\":{\\"trust\\":1,\\"respect\\":0,\\"fear\\":0,\\"affinity\\":1}}"},"finish_reason":"stop"}]}
                """;
        private static final byte[] PCM_RESPONSE = new byte[]{1, 0, (byte) 0xff, (byte) 0xff, 0x34, 0x12};

        private final HttpServer server;
        private final ExecutorService executor;
        private final boolean blockTts;
        private final CountDownLatch blocked = new CountDownLatch(1);
        private final AtomicInteger sttRequests = new AtomicInteger();
        private final AtomicInteger chatRequests = new AtomicInteger();
        private final AtomicInteger ttsRequests = new AtomicInteger();

        private ScriptedVoiceProvider(HttpServer server, ExecutorService executor, boolean blockTts) {
            this.server = server;
            this.executor = executor;
            this.blockTts = blockTts;
        }

        static ScriptedVoiceProvider success() throws IOException {
            return start(false);
        }

        static ScriptedVoiceProvider blockedTts() throws IOException {
            return start(true);
        }

        private static ScriptedVoiceProvider start(boolean blockTts) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "villaigence-orchestration-provider");
                thread.setDaemon(true);
                return thread;
            });
            ScriptedVoiceProvider fixture = new ScriptedVoiceProvider(server, executor, blockTts);
            server.setExecutor(executor);
            server.createContext("/v1/audio/transcriptions", fixture::transcribe);
            server.createContext("/v1/chat/completions", fixture::chat);
            server.createContext("/v1/audio/speech", fixture::synthesize);
            server.start();
            return fixture;
        }

        String url(String path) {
            return "http://127.0.0.1:" + server.getAddress().getPort() + path;
        }

        private void transcribe(HttpExchange exchange) throws IOException {
            sttRequests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            sleep(200L);
            respond(exchange, 200, "application/json", "{\"text\":\"Привет, кузнец\"}".getBytes(StandardCharsets.UTF_8));
        }

        private void chat(HttpExchange exchange) throws IOException {
            int attempt = chatRequests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            sleep(200L);
            respond(exchange, 200, "application/json", (attempt == 1 ? EMPTY_CHAT : SUCCESS_CHAT).getBytes(StandardCharsets.UTF_8));
        }

        private void synthesize(HttpExchange exchange) throws IOException {
            ttsRequests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            if (blockTts) {
                try (exchange) {
                    try {
                        blocked.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return;
            }
            respond(exchange, 200, "audio/pcm;rate=24000;channels=1", PCM_RESPONSE);
        }

        private static void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private static void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(status, body.length);
                exchange.getResponseBody().write(body);
            }
        }

        @Override
        public void close() {
            blocked.countDown();
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
