package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StructuredAiBeliefCandidateMetadataTest {
    @Test
    void validCandidateMetadataIsSeparatedFromVisibleMessage() {
        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse("""
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
    void malformedCandidateMetadataCannotInvalidateMessageOrOtherMetadata() {
        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse("""
                {
                  "message": "Останусь здесь.",
                  "optionalCommand": "stay-here",
                  "relationshipDelta": {"trust":0,"respect":0,"fear":0,"affinity":0},
                  "beliefCandidates": {"unexpected":true}
                }
                """);

        assertEquals("Останусь здесь.", response.message());
        assertEquals("stay-here", response.optionalCommand());
        assertEquals(List.of(), response.beliefCandidates());
    }

    @Test
    void malformedJsonMessageRecoveryNeverRecoversCandidateMetadata() {
        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse("""
                {"message":"Вижу тебя.","beliefCandidates":[}
                """);

        assertEquals("Вижу тебя.", response.message());
        assertEquals(List.of(), response.beliefCandidates());
        assertNull(response.relationshipDelta());
    }
}
