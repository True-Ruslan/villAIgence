package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkingMemoryOrchestratorTest {
    @Test
    void keepsLatestTwelveDialogueMessagesAndBoundsUnicodeContent() {
        List<WorkingMemoryMessage> dialogue = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            dialogue.add(new WorkingMemoryMessage(i % 2 == 0 ? "user" : "assistant", "message-" + i));
        }
        dialogue.set(13, new WorkingMemoryMessage("assistant", "🙂".repeat(1300)));

        WorkingMemoryContext context = WorkingMemoryOrchestrator.compose(dialogue, List.of(), List.of());

        assertEquals(12, context.recentDialogue().size());
        assertEquals("message-2", context.recentDialogue().getFirst().content());
        String finalContent = context.recentDialogue().getLast().content();
        assertEquals(1200, finalContent.codePointCount(0, finalContent.length()));
    }

    @Test
    void independentlyBoundsEpisodicAndSemanticContextAndCopiesInputs() {
        List<String> episodic = new ArrayList<>();
        List<String> semantic = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            episodic.add("episode-" + i);
            semantic.add("semantic-" + i);
        }

        WorkingMemoryContext context = WorkingMemoryOrchestrator.compose(List.of(), episodic, semantic);
        episodic.clear();
        semantic.clear();

        assertEquals(List.of("episode-0", "episode-1", "episode-2", "episode-3", "episode-4", "episode-5"), context.episodicContext());
        assertEquals(List.of("semantic-0", "semantic-1", "semantic-2", "semantic-3", "semantic-4", "semantic-5"), context.semanticContext());
    }

    @Test
    void layeredPromptRendersEpisodicAndSemanticSectionsOnce() {
        WorkingMemoryContext context = WorkingMemoryOrchestrator.compose(
                List.of(),
                List.of("VERIFIED | provenance=SYSTEM_OBSERVED | type=ACTION | confidence=100 | summary=\"did work\""),
                List.of("BELIEF | provenance=PLAYER_TOLD | confidence=80 | statement=\"player claim\"")
        );

        String prompt = WorkingMemoryPromptFormatter.promptSection(context);

        assertTrue(prompt.contains("NPC memory context"));
        assertTrue(prompt.contains("NPC semantic memory"));
        assertEquals(1, occurrences(prompt, "NPC memory context"));
        assertEquals(1, occurrences(prompt, "NPC semantic memory"));
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
