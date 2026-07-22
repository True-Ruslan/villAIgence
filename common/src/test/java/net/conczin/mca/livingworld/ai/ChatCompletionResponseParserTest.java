package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatCompletionResponseParserTest {
    @Test
    void nullContentIsSafeAndRetryableWhenProviderGivesNoTerminalReason() {
        String body = """
                {
                  "id": "gen-null-1",
                  "choices": [{
                    "message": {
                      "role": "assistant",
                      "content": null,
                      "reasoning": "internal reasoning"
                    },
                    "finish_reason": null
                  }]
                }
                """;

        ChatCompletionResponseParser.ParsedCompletion parsed = ChatCompletionResponseParser.parse(body);

        assertNull(parsed.content());
        assertNull(parsed.error());
        assertNull(parsed.finishReason());
        assertEquals("gen-null-1", parsed.generationId());
        assertTrue(parsed.reasoningPresent());
        assertTrue(parsed.retryableEmptyContent());
    }

    @Test
    void lengthLimitedNullContentIsNotBlindlyRetried() {
        String body = """
                {
                  "id": "gen-length-1",
                  "choices": [{
                    "message": {"role": "assistant", "content": null},
                    "finish_reason": "length"
                  }]
                }
                """;

        ChatCompletionResponseParser.ParsedCompletion parsed = ChatCompletionResponseParser.parse(body);

        assertNull(parsed.content());
        assertEquals("length", parsed.finishReason());
        assertFalse(parsed.retryableEmptyContent());
    }

    @Test
    void choiceLevelProviderErrorIsParsedWithoutRequiringContent() {
        String body = """
                {
                  "id": "gen-error-1",
                  "choices": [{
                    "message": {"role": "assistant", "content": null},
                    "finish_reason": "error",
                    "error": {
                      "code": 502,
                      "message": "Provider disconnected mid-generation",
                      "metadata": {"error_type": "provider_unavailable"}
                    }
                  }]
                }
                """;

        ChatCompletionResponseParser.ParsedCompletion parsed = ChatCompletionResponseParser.parse(body);

        assertNull(parsed.content());
        assertEquals("Provider disconnected mid-generation", parsed.error());
        assertEquals("provider_unavailable", parsed.errorType());
        assertEquals("error", parsed.finishReason());
        assertFalse(parsed.retryableEmptyContent());
    }

    @Test
    void validContentRemainsAvailableForStructuredParsing() {
        String body = """
                {
                  "id": "gen-ok-1",
                  "choices": [{
                    "message": {"role": "assistant", "content": "{\\"message\\":\\"Привет!\\",\\"optionalCommand\\":\\"\\"}"},
                    "finish_reason": "stop"
                  }]
                }
                """;

        ChatCompletionResponseParser.ParsedCompletion parsed = ChatCompletionResponseParser.parse(body);

        assertEquals("{\"message\":\"Привет!\",\"optionalCommand\":\"\"}", parsed.content());
        assertNull(parsed.error());
        assertEquals("stop", parsed.finishReason());
        assertFalse(parsed.retryableEmptyContent());
    }
}
