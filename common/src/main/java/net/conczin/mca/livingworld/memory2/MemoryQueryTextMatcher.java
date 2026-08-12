package net.conczin.mca.livingworld.memory2;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure provider-independent lexical relevance signal for one bounded memory query. */
final class MemoryQueryTextMatcher {
    static final int MAX_QUERY_CODE_POINTS = 512;
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> NOISE = Set.of(
            "the", "a", "an", "and", "or", "but", "i", "me", "my", "you", "your",
            "what", "which", "who", "when", "where", "why", "how", "did", "do", "does",
            "tell", "told", "said", "say", "earlier", "before", "remember", "recalled",
            "player", "npc", "replied",
            "и", "а", "но", "или", "я", "мне", "мой", "моя", "мое", "моё", "ты", "тебе",
            "твой", "твоя", "что", "кто", "когда", "где", "почему", "как", "какой", "какая",
            "какое", "какие", "говорил", "говорила", "сказал", "сказала", "раньше", "до",
            "помнишь", "помнить"
    );

    private MemoryQueryTextMatcher() {
    }

    static String boundQuery(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).strip();
        int codePoints = normalized.codePointCount(0, normalized.length());
        if (codePoints <= MAX_QUERY_CODE_POINTS) return normalized;
        return normalized.substring(0, normalized.offsetByCodePoints(0, MAX_QUERY_CODE_POINTS));
    }

    static int score(String queryText, String memorySummary) {
        Set<String> queryTokens = meaningfulTokens(boundQuery(queryText));
        if (queryTokens.isEmpty()) return 0;
        Set<String> memoryTokens = meaningfulTokens(memorySummary);
        if (memoryTokens.isEmpty()) return 0;

        int matches = 0;
        for (String token : queryTokens) {
            if (memoryTokens.contains(token)) matches++;
        }
        if (matches <= 0) return 0;
        if (matches == 1) return 50;
        if (matches == 2) return 80;
        return 100;
    }

    private static Set<String> meaningfulTokens(String value) {
        if (value == null || value.isBlank()) return Set.of();
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        Matcher matcher = TOKEN.matcher(normalized);
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        while (matcher.find()) {
            String token = matcher.group();
            if (NOISE.contains(token)) continue;
            if (token.codePointCount(0, token.length()) < 3 && !token.chars().allMatch(Character::isDigit)) continue;
            tokens.add(token);
        }
        return Set.copyOf(tokens);
    }
}
