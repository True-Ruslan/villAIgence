package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatCompletionRetryPolicyTest {
    @Test
    void retriesExactlyOnceForRetryableEmptyContent() {
        ChatCompletionResponseParser.ParsedCompletion empty = new ChatCompletionResponseParser.ParsedCompletion(
                null, null, null, null, "gen-empty", true
        );

        assertTrue(ChatCompletionRetryPolicy.shouldRetry(empty, 1));
        assertFalse(ChatCompletionRetryPolicy.shouldRetry(empty, 2));
        assertEquals(2, ChatCompletionRetryPolicy.MAX_ATTEMPTS);
    }

    @Test
    void doesNotRetryLengthLimitedCompletion() {
        ChatCompletionResponseParser.ParsedCompletion length = new ChatCompletionResponseParser.ParsedCompletion(
                null, null, null, "length", "gen-length", true
        );

        assertFalse(ChatCompletionRetryPolicy.shouldRetry(length, 1));
    }

    @Test
    void doesNotRetryContentFilteredCompletion() {
        ChatCompletionResponseParser.ParsedCompletion filtered = new ChatCompletionResponseParser.ParsedCompletion(
                null, null, null, "content_filter", "gen-filter", false
        );

        assertFalse(ChatCompletionRetryPolicy.shouldRetry(filtered, 1));
    }

    @Test
    void doesNotRetryExplicitProviderError() {
        ChatCompletionResponseParser.ParsedCompletion error = new ChatCompletionResponseParser.ParsedCompletion(
                null, "Provider unavailable", "provider_unavailable", "error", "gen-error", false
        );

        assertFalse(ChatCompletionRetryPolicy.shouldRetry(error, 1));
    }
}
