package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.List;

/** Pure hard-bounded turn-local Working Memory composition. */
public final class WorkingMemoryOrchestrator {
    public static final int MAX_RECENT_DIALOGUE_MESSAGES = 12;
    public static final int MAX_DIALOGUE_CODE_POINTS = 1200;
    public static final int MAX_EPISODIC_ENTRIES = 6;
    public static final int MAX_SEMANTIC_ENTRIES = 6;

    private WorkingMemoryOrchestrator() {
    }

    public static WorkingMemoryContext compose(
            List<WorkingMemoryMessage> recentDialogue,
            List<String> episodicContext,
            List<String> semanticContext
    ) {
        return new WorkingMemoryContext(
                boundDialogue(recentDialogue),
                boundLines(episodicContext, MAX_EPISODIC_ENTRIES),
                boundLines(semanticContext, MAX_SEMANTIC_ENTRIES)
        );
    }

    private static List<WorkingMemoryMessage> boundDialogue(List<WorkingMemoryMessage> messages) {
        if (messages == null || messages.isEmpty()) return List.of();
        List<WorkingMemoryMessage> normalized = new ArrayList<>(messages.size());
        for (WorkingMemoryMessage message : messages) {
            if (message == null || message.content() == null || message.content().isBlank()) continue;
            String boundedContent = limitCodePoints(message.content().strip(), MAX_DIALOGUE_CODE_POINTS);
            if (!boundedContent.isBlank()) {
                normalized.add(new WorkingMemoryMessage(message.role(), boundedContent));
            }
        }
        if (normalized.size() <= MAX_RECENT_DIALOGUE_MESSAGES) return List.copyOf(normalized);
        return List.copyOf(normalized.subList(normalized.size() - MAX_RECENT_DIALOGUE_MESSAGES, normalized.size()));
    }

    private static List<String> boundLines(List<String> values, int maxResults) {
        if (values == null || values.isEmpty()) return List.of();
        List<String> bounded = new ArrayList<>(Math.min(values.size(), maxResults));
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            bounded.add(value.strip());
            if (bounded.size() >= maxResults) break;
        }
        return List.copyOf(bounded);
    }

    private static String limitCodePoints(String value, int maxCodePoints) {
        if (value.codePointCount(0, value.length()) <= maxCodePoints) return value;
        int end = value.offsetByCodePoints(0, maxCodePoints);
        return value.substring(0, end);
    }
}
