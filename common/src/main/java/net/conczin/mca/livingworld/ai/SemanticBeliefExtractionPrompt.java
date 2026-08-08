package net.conczin.mca.livingworld.ai;

/** Pure prompt contract for optional non-authoritative PLAYER_TOLD BELIEF extraction. */
public final class SemanticBeliefExtractionPrompt {
    private SemanticBeliefExtractionPrompt() {
    }

    public static boolean requiresStructuredResponse(
            boolean actionsEnabled,
            boolean relationshipEnabled,
            boolean extractionEnabled
    ) {
        return actionsEnabled || relationshipEnabled || extractionEnabled;
    }

    public static String instruction(boolean enabled, int maxCandidates) {
        if (!enabled) return "";
        int limit = SemanticBeliefCandidateParser.normalizeMaxCandidates(maxCandidates);
        return "beliefCandidates is an array of at most " + limit
                + " short, durable claims explicitly asserted by the latest player message. "
                + "These are advisory non-authoritative BELIEF candidates only. "
                + "Never include claims originating only from the NPC reply. "
                + "Use [] for greetings, questions, commands, transient chatter, or whenever the latest player message contains no useful durable claim. "
                + "Do not include provenance, source IDs, truth labels, or FACT metadata.";
    }
}
