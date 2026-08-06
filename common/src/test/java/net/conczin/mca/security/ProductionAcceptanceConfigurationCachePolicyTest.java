package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionAcceptanceConfigurationCachePolicyTest {
    @Test
    void productionAcceptanceStagingUsesAnEffectiveConfigurationSafeOverride()
            throws IOException {
        Path buildFile = Path.of("..", "fabric", "build.gradle")
                .toAbsolutePath()
                .normalize();
        Path fixtureBuildFile = Path.of("..", "fabric", "production-acceptance-fixture.gradle")
                .toAbsolutePath()
                .normalize();
        String buildSource = Files.readString(buildFile);
        String fixtureSource = Files.readString(fixtureBuildFile);

        int taskDeclaration = buildSource.indexOf(
                "def stageProductionAcceptanceRuntime = tasks.register('stageProductionAcceptanceRuntime')"
        );
        int fixtureApplication = buildSource.indexOf(
                "apply from: 'production-acceptance-fixture.gradle'"
        );
        assertTrue(taskDeclaration >= 0, "Production staging task declaration is missing");
        assertTrue(
                fixtureApplication > taskDeclaration,
                "The configuration-safe override must be applied after the legacy task is registered"
        );

        String overrideBlock = section(
                fixtureSource,
                "def productionAcceptanceMinecraftVersion = providers.gradleProperty('minecraft_version')",
                "\n"
        );
        assertTrue(
                fixtureSource.contains("stageProductionAcceptanceRuntime.configure"),
                "Fixture script must configure the existing production staging task"
        );
        assertTrue(
                fixtureSource.contains("setActions([])"),
                "Fixture script must remove the legacy execution action before adding the safe action"
        );
        assertFalse(
                fixtureSource.contains("project."),
                "Effective production staging must not query Project during task execution"
        );
        assertTrue(
                fixtureSource.contains("productionAcceptanceModId.get()"),
                "Production staging must consume the declared configuration-time mod id provider"
        );
        assertTrue(
                fixtureSource.contains("productionAcceptanceMinecraftVersion.get()"),
                "Production staging manifest must consume the declared Minecraft version provider"
        );
        assertTrue(
                fixtureSource.contains("productionAcceptanceLoaderVersion.get()"),
                "Production staging manifest must consume the declared loader version provider"
        );
        assertTrue(
                fixtureSource.contains("productionAcceptanceInstallerVersion.get()"),
                "Production staging manifest must consume the declared installer version provider"
        );
        assertTrue(
                overrideBlock.startsWith(
                        "def productionAcceptanceMinecraftVersion = providers.gradleProperty('minecraft_version')"
                ),
                "Configuration-time provider declaration must remain explicit"
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
