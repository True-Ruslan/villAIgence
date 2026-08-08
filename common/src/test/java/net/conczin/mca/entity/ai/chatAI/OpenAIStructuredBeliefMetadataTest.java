package net.conczin.mca.entity.ai.chatAI;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenAIStructuredBeliefMetadataTest {
    @Test
    void structuredContentPreservesBeliefCandidatesAlongsideVisibleMessage() {
        OpenAIChatAI.StructuredResponse response = OpenAIChatAI.parseStructuredContent("""
                {
                  "message": "Понял.",
                  "optionalCommand": "",
                  "relationshipDelta": null,
                  "beliefCandidates": [
                    "Игрок говорит, что северный мост опасен.",
                    "Игрок живёт рядом с мельницей."
                  ]
                }
                """);

        assertEquals("Понял.", response.message());
        assertEquals(List.of(
                "Игрок говорит, что северный мост опасен.",
                "Игрок живёт рядом с мельницей."
        ), response.beliefCandidates());
    }

    @Test
    void providerTransportUsesHardBoundWhileLaterLifecycleCanApplyConfiguredLimit() {
        OpenAIChatAI.StructuredResponse response = OpenAIChatAI.parseStructuredContent("""
                {
                  "message": "Хорошо.",
                  "beliefCandidates": ["1","2","3","4","5","6","7","8","9"]
                }
                """);

        assertEquals(8, response.beliefCandidates().size());
    }

    @Test
    void unusableContentCarriesNoPersistableCandidateMetadata() {
        OpenAIChatAI.StructuredResponse malformed = OpenAIChatAI.parseStructuredContent("""
                {"beliefCandidates":["must not persist"]}
                """);

        assertNull(malformed.message());
        assertEquals(List.of(), malformed.beliefCandidates());
    }

    @Test
    void legacyStructuredResponseConstructorsRemainSourceCompatible() {
        OpenAIChatAI.StructuredResponse twoArg = new OpenAIChatAI.StructuredResponse("hello", "");
        OpenAIChatAI.StructuredResponse threeArg = new OpenAIChatAI.StructuredResponse("hello", "", null);

        assertEquals(List.of(), twoArg.beliefCandidates());
        assertEquals(List.of(), threeArg.beliefCandidates());
    }
}
