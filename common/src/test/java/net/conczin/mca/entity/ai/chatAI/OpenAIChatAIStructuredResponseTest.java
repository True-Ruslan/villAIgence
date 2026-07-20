package net.conczin.mca.entity.ai.chatAI;

import com.google.gson.Gson;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipDelta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenAIChatAIStructuredResponseTest {
    private final Gson gson = new Gson();

    @Test
    void legacyTwoFieldJsonStillParsesWithoutRelationshipDelta() {
        OpenAIChatAI.StructuredResponse response = gson.fromJson(
                "{\"message\":\"hello\",\"optionalCommand\":\"\"}",
                OpenAIChatAI.StructuredResponse.class
        );

        assertEquals("hello", response.message());
        assertEquals("", response.optionalCommand());
        assertNull(response.relationshipDelta());
    }

    @Test
    void newRelationshipDeltaJsonParsesAsSeparateMetadata() {
        OpenAIChatAI.StructuredResponse response = gson.fromJson(
                "{\"message\":\"hello\",\"optionalCommand\":\"follow-player\","
                        + "\"relationshipDelta\":{\"trust\":2,\"respect\":1,\"fear\":-1,\"affinity\":2}}",
                OpenAIChatAI.StructuredResponse.class
        );

        assertEquals("hello", response.message());
        assertEquals("follow-player", response.optionalCommand());
        assertEquals(new LivingWorldRelationshipDelta(2, 1, -1, 2), response.relationshipDelta());
    }
}
