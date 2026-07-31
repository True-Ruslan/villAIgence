package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyToolClosurePolicyTest {
    private static final List<String> REMOVED_LEGACY_PATHS = List.of(
            "scripts/TTS",
            "scripts/all.sh",
            "scripts/fetch_contributors.py",
            "scripts/lang_pre_generation.py",
            "scripts/names",
            "scripts/pirate_translator.py",
            "scripts/skins"
    );

    @Test
    void obsoleteInheritedUtilitiesAreAbsent() {
        Path root = repositoryRoot();
        for (String relative : REMOVED_LEGACY_PATHS) {
            assertFalse(Files.exists(root.resolve(relative)),
                    "Obsolete inherited utility must be removed: " + relative);
        }
    }

    @Test
    void onlyApprovedBuildAndSecurityScriptsRemain() throws IOException {
        String inventory = Files.readString(
                repositoryRoot().resolve("docs/security/APPROVED_SCRIPT_INVENTORY.json")
        );

        for (String required : List.of(
                "gradlew",
                "gradlew.bat",
                "scripts/ci/package-livingworld-release.sh",
                "scripts/ci/repository_security_policy.py",
                "scripts/ci/test_repository_security_policy.py"
        )) {
            assertTrue(inventory.contains("\"path\": \"" + required + "\""),
                    "Required approved script is missing: " + required);
        }

        for (String obsolete : List.of(
                "pirate_translator.py",
                "fetch_contributors.py",
                "lang_pre_generation.py",
                "scripts/TTS/",
                "scripts/names/",
                "scripts/skins/"
        )) {
            assertFalse(inventory.contains(obsolete),
                    "Obsolete utility remains approved: " + obsolete);
        }
    }

    @Test
    void securityWorkflowRetainsWholeTreeManifestEvidence() throws IOException {
        String workflow = Files.readString(
                repositoryRoot().resolve(".github/workflows/security-policy.yml")
        );
        assertTrue(workflow.contains("build/security/tracked-tree-manifest.json"));
        assertTrue(workflow.contains("villaigence-tracked-tree-manifest"));
    }

    private static Path repositoryRoot() {
        return Path.of("..").toAbsolutePath().normalize();
    }
}
