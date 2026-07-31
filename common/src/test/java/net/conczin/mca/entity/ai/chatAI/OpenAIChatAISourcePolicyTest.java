package net.conczin.mca.entity.ai.chatAI;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-boundary regression test that avoids loading Minecraft runtime classes in the pure common test JVM.
 */
class OpenAIChatAISourcePolicyTest {
    @Test
    void ChatUsesValidatedEndpointsExactTrustAndNoAutomaticRedirects() throws IOException {
        Path sourcePath = Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"
        );
        assertTrue(Files.isRegularFile(sourcePath), sourcePath.toAbsolutePath().toString());

        String source = Files.readString(sourcePath);

        assertTrue(source.contains("ProviderEndpoint endpoint"));
        assertTrue(source.contains("endpoint.uri().toURL().openConnection()"));
        assertTrue(source.contains("setInstanceFollowRedirects(false)"));
        assertTrue(source.contains("provider.endpoint().trustedConczin()"));
        assertFalse(source.contains("provider.endpoint().contains(\"conczin.net\")"));
    }
}
