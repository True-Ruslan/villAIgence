package net.conczin.mca.entity.ai.chatAI;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotDialogueGuidanceWiringPolicyTest {
    @Test
    void snapshotSystemDerivesBoundedGuidanceAndPassesItThroughCentralPromptPolicy() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"));
        String compact = source.replaceAll("\\s+", " ");

        int influence = compact.indexOf(
                "PersonalitySocialInfluencePolicy.evaluate(snapshot.personalitySocialSnapshot())");
        int guidance = compact.indexOf(
                "PersonalitySocialDialogueGuidanceRenderer.render(personalitySocialInfluence)");
        int compose = compact.indexOf(
                "SnapshotContextPromptPolicy.compose( snapshot.worldFacts(), PersonalitySocialContextRenderer.render(snapshot.personalitySocialSnapshot()), personalitySocialGuidance, snapshot.operatorAuthoredContext(), snapshot.semanticMemoryContext(), snapshot.contradictionContext(), snapshot.memoryContext() )");
        int structured = compact.indexOf("SemanticBeliefExtractionPrompt.requiresStructuredResponse", compose >= 0 ? compose : 0);

        assertTrue(influence >= 0, "snapshot path must derive closed personality/social influence");
        assertTrue(guidance > influence, "guidance must be rendered from the derived influence");
        assertTrue(compose > guidance, "guidance must enter the centralized prompt policy");
        assertTrue(structured > compose, "authority layers must be composed before provider response instructions");
    }
}
