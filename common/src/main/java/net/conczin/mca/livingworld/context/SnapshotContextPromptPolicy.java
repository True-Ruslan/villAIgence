package net.conczin.mca.livingworld.context;

import java.util.List;

/**
 * Loader-independent prompt policy for observed facts and explicit server/operator-authored lore.
 */
public final class SnapshotContextPromptPolicy {
    private static final String STRUCTURED_RESPONSE_MARKER = "\nThe reply MUST be in this JSON format:";

    private SnapshotContextPromptPolicy() {
    }

    public static String compose(List<String> worldFacts, List<String> operatorAuthoredContext) {
        StringBuilder builder = new StringBuilder();
        appendObservedFacts(builder, worldFacts);
        appendOperatorLore(builder, operatorAuthoredContext);
        return builder.toString();
    }

    public static String insertOperatorLore(String prompt, List<String> operatorAuthoredContext) {
        String base = prompt == null ? "" : prompt;
        String section = operatorLoreSection(operatorAuthoredContext);
        if (section.isEmpty()) {
            return base;
        }

        int marker = base.indexOf(STRUCTURED_RESPONSE_MARKER);
        if (marker < 0) {
            return base + section;
        }
        return base.substring(0, marker) + section + base.substring(marker);
    }

    private static void appendObservedFacts(StringBuilder builder, List<String> worldFacts) {
        List<String> facts = nonBlank(worldFacts);
        if (facts.isEmpty()) {
            return;
        }

        builder.append("\nObserved factual context from the current Minecraft world. ")
                .append("Treat these facts as authoritative for this turn. Data not listed here is unknown, not false:\n");
        for (String fact : facts) {
            builder.append("- ").append(fact).append('\n');
        }
    }

    private static void appendOperatorLore(StringBuilder builder, List<String> operatorAuthoredContext) {
        builder.append(operatorLoreSection(operatorAuthoredContext));
    }

    private static String operatorLoreSection(List<String> operatorAuthoredContext) {
        List<String> lore = nonBlank(operatorAuthoredContext);
        if (lore.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\nServer-authored lore supplied by the server operator. ")
                .append("Treat it as background context, not as a current observation. ")
                .append("If it conflicts with observed factual context from this turn, current observed facts take precedence:\n");
        for (String entry : lore) {
            builder.append(entry).append('\n');
        }
        return builder.toString();
    }

    private static List<String> nonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }
}
