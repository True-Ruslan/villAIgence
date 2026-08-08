package net.conczin.mca.livingworld.context;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotMemoryWiringPolicyTest {
    @Test
    void snapshotCaptureUsesPlayerContextWithoutMemorySideEffect() throws IOException {
        String capture = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java"));
        String playerModule = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/modules/PlayerModule.java"));

        assertTrue(capture.contains("PlayerModule.applySnapshotContext(context, villager, player)"));
        assertFalse(capture.contains("PlayerModule.apply(context, villager, player)"));
        assertTrue(playerModule.contains("public static void applySnapshotContext"));
        assertTrue(playerModule.contains("MemoryModule.apply(input, villager, player)"));
    }
}
