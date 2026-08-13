package net.conczin.mca.livingworld.memory2;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Deterministic bounded lexical relevance used only after Memory 2.0 authority filtering. */
final class MemoryLexicalRelevance {
    private static final int MAX_QUERY_TOKENS = 16;
    private static final int MAX_EVENT_TOKENS = 64;
    private static final int MIN_TOKEN_CODE_POINTS = 4;
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

    private MemoryLexicalRelevance() {
    }

    static int score(String queryText, String eventText) {
        Set<String> queryTokens = tokens(queryText, MAX_QUERY_TOKENS);
        if (queryTokens.isEmpty()) return 0;

        Set<String> eventTokens = tokens(eventText, MAX_EVENT_TOKENS);
        if (eventTokens.isEmpty()) return 0;

        int overlap = 0;
        for (String token : queryTokens) {
            if (eventTokens.contains(token)) overlap++;
        }
        if (overlap == 0) return 0;

        int targetMatches = Math.min(2, queryTokens.size());
        return Math.min(100, (overlap * 100) / targetMatches);
    }

    static boolean hasUsefulQuery(String queryText) {
        return !tokens(queryText, MAX_QUERY_TOKENS).isEmpty();
    }

    private static Set<String> tokens(String text, int limit) {
        if (text == null || text.isBlank() || limit <= 0) return Set.of();

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : TOKEN_SEPARATOR.split(normalized)) {
            if (token.isBlank() || token.codePointCount(0, token.length()) < MIN_TOKEN_CODE_POINTS) continue;
            tokens.add(token);
            if (tokens.size() >= limit) break;
        }
        return Set.copyOf(tokens);
    }
}
