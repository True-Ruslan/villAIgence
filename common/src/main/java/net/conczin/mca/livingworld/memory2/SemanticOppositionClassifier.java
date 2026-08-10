package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Conservative deterministic opposition classifier for an already-bounded Semantic claim pair. */
final class SemanticOppositionClassifier {
    private SemanticOppositionClassifier() {
    }

    static boolean opposes(SemanticMemoryEntry first, SemanticMemoryEntry second) {
        if (first == null || second == null) return false;
        List<String> firstTokens = tokens(first.statement());
        List<String> secondTokens = tokens(second.statement());
        if (firstTokens.isEmpty() || secondTokens.isEmpty() || firstTokens.equals(secondTokens)) return false;

        int firstNegations = negationCount(firstTokens);
        int secondNegations = negationCount(secondTokens);
        if (firstNegations == 1 && secondNegations == 0) {
            return withoutNegation(firstTokens).equals(secondTokens);
        }
        if (secondNegations == 1 && firstNegations == 0) {
            return withoutNegation(secondTokens).equals(firstTokens);
        }
        return false;
    }

    private static List<String> tokens(String statement) {
        String canonical = SemanticMemoryIdentity.canonicalStatement(statement);
        if (canonical.isBlank()) return List.of();
        return List.copyOf(Arrays.asList(canonical.split("\\s+")));
    }

    private static int negationCount(List<String> tokens) {
        int count = 0;
        for (String token : tokens) {
            if (isNegation(token)) count++;
        }
        return count;
    }

    private static List<String> withoutNegation(List<String> tokens) {
        List<String> stripped = new ArrayList<>(tokens.size() - 1);
        boolean removed = false;
        for (String token : tokens) {
            if (!removed && isNegation(token)) {
                removed = true;
                continue;
            }
            stripped.add(token);
        }
        return List.copyOf(stripped);
    }

    private static boolean isNegation(String token) {
        return "not".equals(token) || "не".equals(token);
    }
}
