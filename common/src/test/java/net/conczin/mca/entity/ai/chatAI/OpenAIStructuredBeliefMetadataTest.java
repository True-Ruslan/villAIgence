package net.conczin.mca.entity.ai.chatAI;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIStructuredBeliefMetadataTest {
    @Test
    void openAiTransportPreservesBoundedCandidatesWithoutLoadingMinecraftRuntime() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"
        ));

        assertTrue(source.contains("SemanticBeliefCandidateParser.HARD_MAX_CANDIDATES"));
        assertTrue(source.contains("parsed.beliefCandidates()"));
        assertTrue(source.contains("List<String> beliefCandidates"));
        assertTrue(source.contains("return new StructuredResponse(null, \"\", null, List.of());"));
    }

    @Test
    void legacyStructuredResponseConstructorsRemainSourceCompatible() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"
        ));

        assertTrue(source.contains("public StructuredResponse(@Nullable String message, String optionalCommand)"));
        assertTrue(source.contains("this(message, optionalCommand, null, List.of());"));
        assertTrue(source.contains("this(message, optionalCommand, relationshipDelta, List.of());"));
    }
}
