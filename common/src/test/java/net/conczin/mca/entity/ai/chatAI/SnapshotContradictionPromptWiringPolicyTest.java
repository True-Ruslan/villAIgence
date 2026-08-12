package net.conczin.mca.entity.ai.chatAI;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotContradictionPromptWiringPolicyTest {
    @Test
    void openAiSnapshotPromptIncludesContradictionLayerExactlyOnceBeforeStructuredInstructions() throws IOException {
        String source = Files.readString(Path.of("src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"));
        String compact = source.replaceAll("\\s+", " ");
        String expected = "SnapshotContextPromptPolicy.compose( snapshot.worldFacts(), PersonalitySocialContextRenderer.render(snapshot.personalitySocialSnapshot()), personalitySocialGuidance, snapshot.operatorAuthoredContext(), snapshot.semanticMemoryContext(), snapshot.contradictionContext(), snapshot.memoryContext() )";
        int layered = compact.indexOf(expected);
        int structured = compact.indexOf("SemanticBeliefExtractionPrompt.requiresStructuredResponse", layered >= 0 ? layered : 0);

        assertTrue(layered >= 0);
        assertEquals(1, occurrences(compact, "snapshot.contradictionContext()"));
        assertTrue(structured > layered);
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
