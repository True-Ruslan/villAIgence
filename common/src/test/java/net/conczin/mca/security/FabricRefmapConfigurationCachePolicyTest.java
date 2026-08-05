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

        String taskDeclaration = "def verifyFabricRefmap = tasks.register('verifyFabricRefmap')";
        String nextSection = "\n\ndef productionAcceptanceStageDir =";
        int taskStart = source.indexOf(taskDeclaration);
        int taskEnd = source.indexOf(nextSection, taskStart + taskDeclaration.length());
        assertTrue(taskStart >= 0, "verifyFabricRefmap task is missing");
        assertTrue(taskEnd > taskStart, "verifyFabricRefmap task boundary is malformed");

        String taskBlock = source.substring(taskStart, taskEnd);
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
