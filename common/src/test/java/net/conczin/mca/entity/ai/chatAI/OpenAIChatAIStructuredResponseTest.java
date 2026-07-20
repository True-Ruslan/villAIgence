package net.conczin.mca.entity.ai.chatAI;

import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenAIChatAIStructuredResponseTest {
    @Test
    void legacyTwoFieldJsonStillParsesWithoutRelationshipDelta() throws Exception {
        OpenAIChatAI.StructuredResponse response = parseStructuredContent(
                "{\"message\":\"hello\",\"optionalCommand\":\"\"}"
        );

        assertEquals("hello", response.message());
        assertEquals("", response.optionalCommand());
        assertNull(response.relationshipDelta());
    }

    @Test
    void newRelationshipDeltaJsonParsesAsSeparateMetadata() throws Exception {
        OpenAIChatAI.StructuredResponse response = parseStructuredContent(
                "{\"message\":\"hello\",\"optionalCommand\":\"follow-player\","
                        + "\"relationshipDelta\":{\"trust\":2,\"respect\":1,\"fear\":-1,\"affinity\":2}}"
        );

        assertEquals("hello", response.message());
        assertEquals("follow-player", response.optionalCommand());
        assertEquals(new LivingWorldRelationshipDelta(2, 1, -1, 2), response.relationshipDelta());
    }

    private static OpenAIChatAI.StructuredResponse parseStructuredContent(String content) throws Exception {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"");
        String providerBody = "{\"choices\":[{\"message\":{\"content\":\"" + escaped + "\"}}]}";
        Method parseAnswer = OpenAIChatAI.class.getDeclaredMethod("parseAnswer", String.class);
        parseAnswer.setAccessible(true);
        OpenAIChatAI.Answer answer = (OpenAIChatAI.Answer) parseAnswer.invoke(null, providerBody);
        return answer.answer();
    }
}
