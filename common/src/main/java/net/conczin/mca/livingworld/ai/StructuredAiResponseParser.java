package net.conczin.mca.livingworld.ai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Defensive parser for LLM-visible structured responses.
 *
 * <p>The user-visible message is intentionally isolated from optional metadata so malformed
 * commands or relationship deltas can never leak raw JSON into chat or persistent memory.</p>
 */
public final class StructuredAiResponseParser {
    private StructuredAiResponseParser() {
    }

    public static ParsedResponse parse(@Nullable String content) {
        if (content == null) return new ParsedResponse(null, "", null);

        String normalized = stripMarkdownFences(content).trim();
        JsonSpan json = findFirstJsonObject(normalized);
        if (json != null) {
            JsonObject object = parseObject(json.json());
            if (object != null) {
                String message = stringField(object, "message");
                String optionalCommand = stringField(object, "optionalCommand");
                LivingWorldRelationshipDelta relationshipDelta = parseRelationshipDelta(object.get("relationshipDelta"));

                if (message != null) {
                    return new ParsedResponse(message.trim(), optionalCommand == null ? "" : optionalCommand.trim(), relationshipDelta);
                }
            }

            String prefix = normalized.substring(0, json.start()).trim();
            if (!prefix.isEmpty()) return new ParsedResponse(prefix, "", null);
            return new ParsedResponse(null, "", null);
        }

        return new ParsedResponse(normalized.isEmpty() ? null : normalized, "", null);
    }

    private static String stripMarkdownFences(String content) {
        return content
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "");
    }

    @Nullable
    private static JsonObject parseObject(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static String stringField(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) return null;
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        return primitive.isString() ? primitive.getAsString() : null;
    }

    @Nullable
    private static LivingWorldRelationshipDelta parseRelationshipDelta(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (!element.isJsonObject()) return null;

        JsonObject object = element.getAsJsonObject();
        Integer trust = integerField(object, "trust");
        Integer respect = integerField(object, "respect");
        Integer fear = integerField(object, "fear");
        Integer affinity = integerField(object, "affinity");
        if (trust == null || respect == null || fear == null || affinity == null) return null;
        return new LivingWorldRelationshipDelta(trust, respect, fear, affinity);
    }

    @Nullable
    private static Integer integerField(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) return 0;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return null;
        try {
            return new BigDecimal(value.getAsString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static JsonSpan findFirstJsonObject(String text) {
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        int start = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}' && depth > 0) {
                depth--;
                if (depth == 0 && start >= 0) {
                    return new JsonSpan(start, i + 1, text.substring(start, i + 1));
                }
            }
        }
        return null;
    }

    public record ParsedResponse(
            @Nullable String message,
            String optionalCommand,
            @Nullable LivingWorldRelationshipDelta relationshipDelta
    ) {
    }

    private record JsonSpan(int start, int end, String json) {
    }
}
