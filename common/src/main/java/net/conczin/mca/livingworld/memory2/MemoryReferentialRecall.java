package net.conczin.mca.livingworld.memory2;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic bounded recall hint for explicit requests to recover opaque player-told markers.
 *
 * <p>This is not semantic search and grants no new authority. It is evaluated only on events that
 * already passed the caller's NPC/player eligibility boundary.</p>
 */
final class MemoryReferentialRecall {
    private static final int MAX_QUERY_TOKENS = 24;
    private static final int MAX_SOURCE_TOKENS = 48;
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern COMPACT_OPAQUE_MARKER = Pattern.compile(
            "(?i)(?<![a-z0-9])"
                    + "(?=[a-z0-9_-]*[a-z])"
                    + "(?=[a-z0-9_-]*\\d)"
                    + "[a-z0-9]+(?:[-_][a-z0-9]+)+"
                    + "(?![a-z0-9])"
    );
    private static final Pattern SPOKEN_OPAQUE_MARKER = Pattern.compile(
            "(?i)(?<![a-z])[a-z]{3,}\\s+[a-z]{3,}\\s+\\d{2,}(?!\\d)"
    );

    private static final Set<String> MARKER_NOUN_STEMS = Set.of(
            "маркер", "marker", "токен", "token", "код", "code"
    );
    private static final Set<String> RECALL_STEMS = Set.of(
            "помн", "назов", "ранее", "раньше", "сообщ", "сказ", "говор",
            "remember", "recall", "name", "earlier", "previous", "told", "said"
    );
    private static final Set<String> STORAGE_STEMS = Set.of(
            "запомн", "сохран", "remember", "save", "keep"
    );

    private MemoryReferentialRecall() {
    }

    static int score(String queryText, MemoryEvent event) {
        if (event == null
                || event.type() != MemoryEvent.Type.DIALOGUE
                || event.provenance() != MemoryEvent.Provenance.PLAYER_TOLD
                || !hasRecallIntent(queryText)) {
            return 0;
        }

        String playerText = event.dialogue() == null
                ? event.summary()
                : event.dialogue().playerMessage();
        return hasStorageCue(playerText) && containsOpaqueMarker(playerText) ? 100 : 0;
    }

    static boolean hasRecallIntent(String queryText) {
        Set<String> tokens = tokens(queryText, MAX_QUERY_TOKENS);
        return containsStem(tokens, MARKER_NOUN_STEMS) && containsStem(tokens, RECALL_STEMS);
    }

    static boolean hasStorageCue(String text) {
        return containsStem(tokens(text, MAX_SOURCE_TOKENS), STORAGE_STEMS);
    }

    static boolean containsOpaqueMarker(String text) {
        if (text == null || text.isBlank()) return false;
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        return COMPACT_OPAQUE_MARKER.matcher(normalized).find()
                || SPOKEN_OPAQUE_MARKER.matcher(normalized).find();
    }

    private static boolean containsStem(Set<String> tokens, Set<String> stems) {
        for (String token : tokens) {
            for (String stem : stems) {
                if (token.startsWith(stem)) return true;
            }
        }
        return false;
    }

    private static Set<String> tokens(String text, int limit) {
        if (text == null || text.isBlank() || limit <= 0) return Set.of();
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : TOKEN_SEPARATOR.split(normalized)) {
            if (token.isBlank()) continue;
            tokens.add(token);
            if (tokens.size() >= limit) break;
        }
        return Set.copyOf(tokens);
    }
}
