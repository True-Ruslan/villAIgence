package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionAcceptanceConfigurationCachePolicyTest {
    @Test
    void productionAcceptanceStagingDoesNotResolveProjectStateDuringTaskExecution()
            throws IOException {
        Path buildFile = Path.of("..", "fabric", "build.gradle")
                .toAbsolutePath()
                .normalize();
        String source = Files.readString(buildFile);

        String taskBlock = section(
                source,
                "def stageProductionAcceptanceRuntime = tasks.register('stageProductionAcceptanceRuntime')",
                "\n\ntasks.named('check')"
        );

        assertFalse(
                taskBlock.contains("project.delete("),
                "Production staging must use injected filesystem operations instead of Task.project during execution"
        );
        assertFalse(
                taskBlock.contains("project.property("),
                "Production staging must use configuration-time providers instead of Task.project during execution"
        );
        assertTrue(
                taskBlock.contains("productionAcceptanceModId.get()"),
                "Production staging must consume the declared configuration-time mod id provider"
        );
        assertTrue(
                taskBlock.contains("productionAcceptanceMinecraftVersion.get()"),
                "Production staging manifest must consume the declared Minecraft version provider"
        );
        assertTrue(
                taskBlock.contains("productionAcceptanceLoaderVersion.get()"),
                "Production staging manifest must consume the declared loader version provider"
        );
        assertTrue(
                taskBlock.contains("productionAcceptanceInstallerVersion.get()"),
                "Production staging manifest must consume the declared installer version provider"
        );
    }

    @Test
    void fixtureStagingExtensionDoesNotResolveProjectStateDuringTaskExecution()
            throws IOException {
        Path buildFile = Path.of("..", "fabric", "production-acceptance-fixture.gradle")
                .toAbsolutePath()
                .normalize();
        String source = Files.readString(buildFile);

        String taskBlock = section(
                source,
                "tasks.named('stageProductionAcceptanceRuntime')",
                "\n}"
        );
        assertFalse(
                taskBlock.contains("project."),
                "Fixture staging must not query Project during task execution"
        );
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        assertTrue(start >= 0, "Missing source section: " + startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(end > start, "Malformed source section: " + startMarker);
        return source.substring(start, end);
    }
}
