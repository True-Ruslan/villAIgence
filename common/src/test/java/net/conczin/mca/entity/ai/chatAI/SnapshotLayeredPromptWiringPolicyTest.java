package net.conczin.mca.entity.ai.chatAI;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotLayeredPromptWiringPolicyTest {
    @Test
    void snapshotSystemUsesDirectLayeredPolicyBeforeStructuredResponseInstructions() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"));
        String compact = source.replaceAll("\\s+", " ");

        String layeredCall = "SnapshotContextPromptPolicy.compose( snapshot.worldFacts(), PersonalitySocialContextRenderer.render(snapshot.personalitySocialSnapshot()), personalitySocialGuidance, snapshot.operatorAuthoredContext(), snapshot.semanticMemoryContext(), snapshot.contradictionContext(), snapshot.memoryContext() )";
        int layered = compact.indexOf(layeredCall);
        int structured = compact.indexOf("SemanticBeliefExtractionPrompt.requiresStructuredResponse", layered >= 0 ? layered : 0);

        assertTrue(layered >= 0);
        assertTrue(structured > layered);
        assertFalse(source.contains("if (!snapshot.worldFacts().isEmpty())"));
        assertFalse(source.contains("Observed factual context from the current Minecraft world. Treat these facts as authoritative for this turn"));
        assertFalse(source.contains("Current NPC personality: \" +"));
        assertFalse(source.contains("Current directed social state toward the current NPC counterpart: \" +"));
    }

    @Test
    void obsoleteLoreMixinIsNotRegistered() throws IOException {
        String mixins = Files.readString(Path.of("src/main/resources/mca.mixins.json"));
        assertFalse(mixins.contains("MixinOpenAIChatAI"));
    }
}
