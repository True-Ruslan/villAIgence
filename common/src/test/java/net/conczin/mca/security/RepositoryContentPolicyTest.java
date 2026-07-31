package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryContentPolicyTest {
    @Test
    void inheritedRepositoriesAreRestrictedToRequiredGroups() throws IOException {
        Path root = Path.of("..").toAbsolutePath().normalize();
        String commonConvention = Files.readString(
                root.resolve("buildSrc/src/main/groovy/multiloader-common.gradle")
        );
        String fabricBuild = Files.readString(root.resolve("fabric/build.gradle"));

        assertFalse(
                commonConvention.contains("maven.blamejared.com"),
                "The inherited broad BlameJared resolver is unused and must not remain available"
        );
        assertTrue(
                commonConvention.contains("includeGroup('maven.modrinth')"),
                "Modrinth must be restricted to the maven.modrinth group"
        );
        assertTrue(
                fabricBuild.contains("includeGroup('de.maxhenkel.voicechat')"),
                "MaxHenkel repository must be restricted to voicechat artifacts"
        );
    }
}
