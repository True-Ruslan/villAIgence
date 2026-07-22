package net.conczin.mca.livingworld.memory2;

import java.util.List;

/** Immutable turn-local composition of recent dialogue and selected durable memory layers. */
public record WorkingMemoryContext(
        List<WorkingMemoryMessage> recentDialogue,
        List<String> episodicContext,
        List<String> semanticContext
) {
    public WorkingMemoryContext {
        recentDialogue = recentDialogue == null ? List.of() : List.copyOf(recentDialogue);
        episodicContext = episodicContext == null ? List.of() : List.copyOf(episodicContext);
        semanticContext = semanticContext == null ? List.of() : List.copyOf(semanticContext);
    }
}
