package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Conservative deterministic opposition classifier for an already-bounded Semantic claim pair. */
final class SemanticOppositionClassifier {
    private static final Pattern BARE_NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

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
        return isSoleNumericConflict(firstTokens, secondTokens);
    }

    /**
     * Two statements oppose on a numeric conflict only when they have the same token count,
     * differ at exactly one position, and that position holds two bare numeric tokens with
     * different parsed values. Equal values written differently (e.g. "04" vs "4") are not a
     * conflict, and any non-numeric or multi-position difference is left unclassified.
     */
    private static boolean isSoleNumericConflict(List<String> firstTokens, List<String> secondTokens) {
        if (firstTokens.size() != secondTokens.size()) return false;

        int diffIndex = -1;
        for (int i = 0; i < firstTokens.size(); i++) {
            if (!firstTokens.get(i).equals(secondTokens.get(i))) {
                if (diffIndex != -1) return false;
                diffIndex = i;
            }
        }
        if (diffIndex == -1) return false;

        Double firstValue = parseBareNumber(firstTokens.get(diffIndex));
        Double secondValue = parseBareNumber(secondTokens.get(diffIndex));
        return firstValue != null && secondValue != null && !firstValue.equals(secondValue);
    }

    private static Double parseBareNumber(String token) {
        if (!BARE_NUMBER.matcher(token).matches()) return null;
        try {
            return Double.valueOf(token);
        } catch (NumberFormatException e) {
            return null;
        }
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
