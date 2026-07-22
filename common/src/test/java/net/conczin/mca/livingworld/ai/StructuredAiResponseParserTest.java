package net.conczin.mca.livingworld.ai;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StructuredAiResponseParserTest {
    @Test
    void keepsVisibleMessageWhenRelationshipDeltaContainsNonNumericField() {
        String content = """
                {"message":"Да нормально всё, не ною я.","optionalCommand":"","relationshipDelta":{"trust":0,"respect":1,"fear":"none","affinity":0}}
                """;

        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse(content);

        assertEquals("Да нормально всё, не ною я.", response.message());
        assertEquals("", response.optionalCommand());
        assertNull(response.relationshipDelta());
    }

    @Test
    void recoversOnlyMessageWhenWholeJsonIsMalformedByBareMetadataToken() {
        String content = """
                {
                  "message": "Да, я житель!",
                  "optionalCommand": "",
                  "relationshipDelta": {
                    "trust": 0,
                    "respect": 0,
                    "fear": INVALID_VALUE,
                    "affinity": 0
                  }
                }
                """;

        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse(content);

        assertEquals("Да, я житель!", response.message());
        assertEquals("", response.optionalCommand());
        assertNull(response.relationshipDelta());
        assertFalse(response.message().contains("relationshipDelta"));
        assertFalse(response.message().contains("INVALID_VALUE"));
    }

    @Test
    void recoversEscapedQuotesAndUnicodeFromMalformedObject() {
        String content = """
                {"message":"Он сказал: \\\"Привет!\\\" — всё хорошо ☺","optionalCommand":"","relationshipDelta":{"fear":INVALID}}
                """;

        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse(content);

        assertEquals("Он сказал: \"Привет!\" — всё хорошо ☺", response.message());
        assertNull(response.relationshipDelta());
    }

    @Test
    void invalidOptionalCommandTypeDoesNotInvalidateMessage() {
        String content = """
                {"message":"Останусь здесь.","optionalCommand":{"unexpected":true},"relationshipDelta":{"trust":0,"respect":0,"fear":0,"affinity":0}}
                """;

        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse(content);

        assertEquals("Останусь здесь.", response.message());
        assertEquals("", response.optionalCommand());
        assertEquals(LivingWorldRelationshipDelta.NONE, response.relationshipDelta());
    }

    @Test
    void parsesValidRelationshipDelta() {
        String content = """
                {"message":"Ладно.","optionalCommand":"stay-here","relationshipDelta":{"trust":-1,"respect":0,"fear":2,"affinity":1}}
                """;

        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse(content);

        assertEquals("Ладно.", response.message());
        assertEquals("stay-here", response.optionalCommand());
        assertEquals(new LivingWorldRelationshipDelta(-1, 0, 2, 1), response.relationshipDelta());
    }

    @Test
    void extractsStructuredJsonFromMarkdownFenceWithoutLeakingJson() {
        String content = """
                ```json
                {"message":"Привет!","optionalCommand":"","relationshipDelta":{"trust":0,"respect":0,"fear":0,"affinity":0}}
                ```
                """;

        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse(content);

        assertEquals("Привет!", response.message());
        assertFalse(response.message().contains("relationshipDelta"));
    }

    @Test
    void fallsBackToPlainTextWithoutTrailingJsonObject() {
        String content = "Привет, человек. {\"relationshipDelta\":{\"fear\":\"oops\"}}";

        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse(content);

        assertEquals("Привет, человек.", response.message());
        assertNull(response.relationshipDelta());
    }

    @Test
    void unrecoverableJsonReturnsNoVisibleMessage() {
        String content = """
                {"optionalCommand":"","relationshipDelta":{"fear":INVALID_VALUE}}
                """;

        StructuredAiResponseParser.ParsedResponse response = StructuredAiResponseParser.parse(content);

        assertNull(response.message());
        assertEquals("", response.optionalCommand());
        assertNull(response.relationshipDelta());
    }
}
