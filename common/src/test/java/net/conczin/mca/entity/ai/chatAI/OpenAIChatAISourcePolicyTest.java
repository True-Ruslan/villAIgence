package net.conczin.mca.entity.ai.chatAI;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-boundary regression tests that avoid loading Minecraft runtime classes in the pure common test JVM.
 */
class OpenAIChatAISourcePolicyTest {
    @Test
    void ChatUsesValidatedEndpointsExactTrustAndNoAutomaticRedirects() throws IOException {
        String chatSource = readSource(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"
        );
        String transportSource = readSource(
                "src/main/java/net/conczin/mca/livingworld/ai/ChatCompletionHttpClient.java"
        );

        assertTrue(chatSource.contains("ProviderEndpoint endpoint"));
        assertTrue(transportSource.contains("endpoint.uri().toURL().openConnection()"));
        assertTrue(transportSource.contains("setInstanceFollowRedirects(false)"));
        assertTrue(chatSource.contains("provider.endpoint().trustedConczin()"));
        assertFalse(chatSource.contains("provider.endpoint().contains(\"conczin.net\")"));
    }

    @Test
    void ChatDelegatesToOneBoundedDeadlineAwareTransportAndArbitraryGetHelperIsAbsent()
            throws IOException {
        String chatSource = readSource(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"
        );
        String transportSource = readSource(
                "src/main/java/net/conczin/mca/livingworld/ai/ChatCompletionHttpClient.java"
        );

        assertTrue(chatSource.contains("ChatCompletionHttpClient.post("));
        assertFalse(chatSource.contains("postOnce("));
        assertFalse(chatSource.contains("new DataOutputStream("));

        assertTrue(transportSource.contains("BoundedResponseReader.readUtf8"));
        assertTrue(transportSource.contains("ProviderResponseLimits.CHAT_JSON_BYTES"));
        assertTrue(transportSource.contains("ProviderResponseLimits.ERROR_BODY_BYTES"));
        assertTrue(transportSource.contains("AiRequestDeadline.start("));
        assertTrue(transportSource.contains("deadline.boundedTimeoutMillis("));
        assertFalse(transportSource.contains("readAllBytes("));

        assertFalse(chatSource.contains("IOUtils.toString"));
        assertFalse(chatSource.contains("public static String verify(String encodedURL)"));
        assertFalse(chatSource.contains("URI.create(encodedURL)"));
    }

    private static String readSource(String relativePath) throws IOException {
        Path sourcePath = Path.of(relativePath);
        assertTrue(Files.isRegularFile(sourcePath), sourcePath.toAbsolutePath().toString());
        return Files.readString(sourcePath);
    }
}
