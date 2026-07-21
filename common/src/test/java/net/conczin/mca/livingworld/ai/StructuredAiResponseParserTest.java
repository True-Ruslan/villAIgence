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
}
