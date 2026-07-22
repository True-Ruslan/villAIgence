package net.conczin.mca.livingworld.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

/**
 * Parses the outer OpenAI-compatible Chat Completions response envelope.
 *
 * <p>This class deliberately stops at provider metadata and raw message content. The NPC-visible
 * payload is sanitized separately by {@link StructuredAiResponseParser}.</p>
 */
public final class ChatCompletionResponseParser {
    private ChatCompletionResponseParser() {
    }

    public static ParsedCompletion parse(String body) {
        if (body == null || body.isBlank()) {
            return new ParsedCompletion(null, "AI provider returned an empty response body", "empty_response", null, null, false);
        }

        JsonElement rootElement;
        try {
            rootElement = JsonParser.parseString(body);
        } catch (RuntimeException e) {
            return new ParsedCompletion(null, "AI provider returned invalid JSON", "invalid_json", null, null, false);
        }
        if (!rootElement.isJsonObject()) {
            return new ParsedCompletion(null, "AI provider returned a non-object JSON response", "invalid_response", null, null, false);
        }

        JsonObject root = rootElement.getAsJsonObject();
        String generationId = readString(root.get("id"));
        ErrorInfo rootError = parseError(root.get("error"));

        JsonObject choice = firstChoice(root.get("choices"));
        String finishReason = choice == null ? null : readString(choice.get("finish_reason"));
        ErrorInfo choiceError = choice == null ? ErrorInfo.NONE : parseError(choice.get("error"));
        ErrorInfo error = choiceError.message() != null ? choiceError : rootError;

        JsonObject message = null;
        if (choice != null) {
            JsonElement messageElement = choice.get("message");
            if (messageElement != null && messageElement.isJsonObject()) {
                message = messageElement.getAsJsonObject();
            }
        }

        String content = message == null ? null : readString(message.get("content"));
        if (content != null && content.isBlank()) content = null;
        boolean reasoningPresent = hasReasoning(message);

        return new ParsedCompletion(
                content,
                error.message(),
                error.errorType(),
                finishReason,
                generationId,
                reasoningPresent
        );
    }

    @Nullable
    private static JsonObject firstChoice(@Nullable JsonElement choicesElement) {
        if (choicesElement == null || !choicesElement.isJsonArray()) return null;
        JsonArray choices = choicesElement.getAsJsonArray();
        if (choices.isEmpty() || !choices.get(0).isJsonObject()) return null;
        return choices.get(0).getAsJsonObject();
    }

    private static boolean hasReasoning(@Nullable JsonObject message) {
        if (message == null) return false;
        String reasoning = readString(message.get("reasoning"));
        if (reasoning != null && !reasoning.isBlank()) return true;
        JsonElement details = message.get("reasoning_details");
        return details != null && details.isJsonArray() && !details.getAsJsonArray().isEmpty();
    }

    private static ErrorInfo parseError(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) return ErrorInfo.NONE;
        if (element.isJsonObject()) {
            JsonObject error = element.getAsJsonObject();
            String message = clean(readString(error.get("message")));
            String type = clean(readString(error.get("error_type")));
            JsonElement metadataElement = error.get("metadata");
            if ((type == null || type.isBlank()) && metadataElement != null && metadataElement.isJsonObject()) {
                type = clean(readString(metadataElement.getAsJsonObject().get("error_type")));
            }
            if (message == null || message.isBlank()) {
                String code = clean(readString(error.get("code")));
                if (code != null && !code.isBlank()) message = code;
            }
            return new ErrorInfo(message, type);
        }
        return new ErrorInfo(clean(readString(element)), null);
    }

    @Nullable
    private static String readString(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) return null;
        if (!element.getAsJsonPrimitive().isString() && !element.getAsJsonPrimitive().isNumber()) return null;
        try {
            return element.getAsString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static String clean(@Nullable String value) {
        if (value == null) return null;
        String cleaned = value.trim().replace('\n', ' ').replace('\r', ' ');
        return cleaned.isBlank() ? null : cleaned;
    }

    private record ErrorInfo(@Nullable String message, @Nullable String errorType) {
        private static final ErrorInfo NONE = new ErrorInfo(null, null);
    }

    public record ParsedCompletion(
            @Nullable String content,
            @Nullable String error,
            @Nullable String errorType,
            @Nullable String finishReason,
            @Nullable String generationId,
            boolean reasoningPresent
    ) {
        public boolean retryableEmptyContent() {
            if (content != null && !content.isBlank()) return false;
            if (error != null && !error.isBlank()) return false;
            if (finishReason == null || finishReason.isBlank()) return true;
            return "stop".equalsIgnoreCase(finishReason);
        }
    }
}
