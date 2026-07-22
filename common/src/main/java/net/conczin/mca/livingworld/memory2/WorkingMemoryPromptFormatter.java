package net.conczin.mca.livingworld.memory2;

/** Renders durable Working Memory layers exactly once while recent dialogue remains chat messages. */
public final class WorkingMemoryPromptFormatter {
    private WorkingMemoryPromptFormatter() {
    }

    public static String promptSection(WorkingMemoryContext context) {
        if (context == null) return "";
        return MemoryContextFormatter.promptSection(context.episodicContext())
                + SemanticMemoryContextFormatter.promptSection(context.semanticContext());
    }
}
