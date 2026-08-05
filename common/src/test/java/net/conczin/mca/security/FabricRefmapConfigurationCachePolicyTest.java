package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricRefmapConfigurationCachePolicyTest {
    @Test
    void refmapVerifierDoesNotResolveProjectStateDuringTaskExecution() throws IOException {
        Path buildFile = Path.of("..", "fabric", "build.gradle").toAbsolutePath().normalize();
        String source = Files.readString(buildFile);

        int taskStart = source.indexOf("def verifyFabricRefmap = tasks.register('verifyFabricRefmap')");
        int nextTask = source.indexOf("\ntasks.", taskStart + 1);
        assertTrue(taskStart >= 0, "verifyFabricRefmap task is missing");
        assertTrue(nextTask > taskStart, "verifyFabricRefmap task boundary is malformed");

        String taskBlock = source.substring(taskStart, nextTask);
        assertFalse(
                taskBlock.contains("project.property("),
                "verifyFabricRefmap must not query Project during execution because build-commit-artifacts uses configuration cache"
        );
        assertTrue(
                taskBlock.contains("def productionModId = refmapEntryName.substring("),
                "verifyFabricRefmap must derive the already-captured mod id from configuration-time input"
        );
    }
}
