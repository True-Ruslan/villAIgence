package net.conczin.mca.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedTextTurnSnapshotPolicyTest {
    @Test
    void authenticatedTextTurnCapturesQueryAwareSnapshotBeforeAsyncProviderWork() throws IOException {
        Path source = repositoryRoot().resolve(
                "common/src/main/java/net/conczin/mca/network/AuthenticatedTextTurn.java"
        );
        String java = Files.readString(source);

        String normalize = "String normalizedMessage = AuthenticatedTextTurnCore.normalize(message);";
        String capture = "LivingWorldContextCapture.capture(player, villager, normalizedMessage)";
        String async = "CompletableFuture.runAsync";
        String snapshotAnswer = "ChatAI.answer(server, player, villager, text, snapshot)";

        assertTrue(java.contains(capture),
                "authenticated text turns must capture Memory 2.0 retrieval context with the current message");
        assertTrue(java.contains(snapshotAnswer),
                "authenticated text turns must use the snapshot-aware ChatAI path");

        int normalizeIndex = java.indexOf(normalize);
        int captureIndex = java.indexOf(capture);
        int asyncIndex = java.indexOf(async);
        assertTrue(normalizeIndex >= 0 && captureIndex > normalizeIndex,
                "the normalized current message must exist before snapshot capture");
        assertTrue(asyncIndex > captureIndex,
                "snapshot capture must happen on the caller/server thread before async provider work");
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.isRegularFile(current.resolve("gradlew"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.isRegularFile(parent.resolve("gradlew"))) {
            return parent;
        }
        throw new IllegalStateException("Unable to locate repository root from " + current);
    }
}
