package net.conczin.mca.livingworld.ai;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Parses optional non-authoritative semantic BELIEF candidate metadata. */
public final class SemanticBeliefCandidateParser {
    public static final int DEFAULT_MAX_CANDIDATES = 3;
    public static final int HARD_MAX_CANDIDATES = 8;
    public static final int MAX_STATEMENT_CODE_POINTS = 240;

    private SemanticBeliefCandidateParser() {
    }

    public static List<String> parse(@Nullable String jsonArray, int maxCandidates) {
        if (jsonArray == null || jsonArray.isBlank()) return List.of();

        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(jsonArray);
        } catch (RuntimeException ignored) {
            return List.of();
        }
        if (!parsed.isJsonArray()) return List.of();

        int limit = normalizeMaxCandidates(maxCandidates);
        List<String> result = new ArrayList<>(limit);
        Set<String> seen = new LinkedHashSet<>();
        for (JsonElement element : parsed.getAsJsonArray()) {
            if (result.size() >= limit) break;
            if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) continue;
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (!primitive.isString()) continue;

            String normalized = normalizeStatement(primitive.getAsString());
            if (normalized.isEmpty() || !seen.add(normalized)) continue;
            result.add(normalized);
        }
        return List.copyOf(result);
    }

    public static int normalizeMaxCandidates(int maxCandidates) {
        if (maxCandidates <= 0) return DEFAULT_MAX_CANDIDATES;
        return Math.min(maxCandidates, HARD_MAX_CANDIDATES);
    }

    private static String normalizeStatement(String statement) {
        String nfkc = Normalizer.normalize(statement == null ? "" : statement, Normalizer.Form.NFKC);
        StringBuilder normalized = new StringBuilder(nfkc.length());
        boolean pendingSpace = false;
        for (int offset = 0; offset < nfkc.length();) {
            int codePoint = nfkc.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint) || Character.isISOControl(codePoint)) {
                pendingSpace = normalized.length() > 0;
                continue;
            }
            if (pendingSpace) {
                normalized.append(' ');
                pendingSpace = false;
            }
            normalized.appendCodePoint(codePoint);
        }

        String value = normalized.toString().trim();
        int count = value.codePointCount(0, value.length());
        if (count <= MAX_STATEMENT_CODE_POINTS) return value;
        int end = value.offsetByCodePoints(0, MAX_STATEMENT_CODE_POINTS);
        return value.substring(0, end).trim();
    }
}
