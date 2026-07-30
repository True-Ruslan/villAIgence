package net.conczin.mca.entity.ai.chatAI;

import net.conczin.mca.livingworld.ai.ProviderEndpoint;
import net.conczin.mca.livingworld.ai.ProviderEndpointPolicy;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenAIChatAIHttpPolicyTest {
    @Test
    void authenticatedChatConnectionUsesValidatedEndpointAndDisablesRedirects() throws Exception {
        ProviderEndpoint endpoint = ProviderEndpointPolicy.parse(
                "https://api.openai.com/v1/chat/completions",
                false
        );

        HttpURLConnection connection = OpenAIChatAI.getHttpURLConnection(endpoint, 1_000, 2_000);
        try {
            assertFalse(connection.getInstanceFollowRedirects());
            assertEquals("api.openai.com", connection.getURL().getHost());
            assertEquals(1_000, connection.getConnectTimeout());
            assertEquals(2_000, connection.getReadTimeout());
        } finally {
            connection.disconnect();
        }
    }
}
