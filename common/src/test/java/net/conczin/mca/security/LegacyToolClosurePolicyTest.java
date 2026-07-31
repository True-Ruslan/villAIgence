package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
            "scripts/requirements.txt",
            "scripts/skins"
    );

    private static final List<String> REMOVED_REFERENCE_TOKENS = List.of(
            "scripts/TTS",
            "scripts/all.sh",
            "fetch_contributors.py",
            "lang_pre_generation.py",
            "scripts/names",
            "pirate_translator.py",
            "scripts/requirements.txt",
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

        long approvedPathCount = inventory.lines()
                .filter(line -> line.contains("\"path\":"))
                .count();
        assertTrue(approvedPathCount == 5,
                "Approved script inventory must contain exactly five reviewed launchers");

        for (String obsolete : REMOVED_REFERENCE_TOKENS) {
            assertFalse(inventory.contains(obsolete),
                    "Obsolete utility remains approved: " + obsolete);
        }
    }

    @Test
    void buildCiAndReleaseCannotReferenceRemovedUtilities() throws IOException {
        Path root = repositoryRoot();
        List<Path> surfaces = new ArrayList<>(List.of(
                root.resolve("build.gradle"),
                root.resolve("settings.gradle"),
                root.resolve("gradle.properties"),
                root.resolve("common/build.gradle"),
                root.resolve("fabric/build.gradle"),
                root.resolve("neoforge/build.gradle")
        ));
        collectFiles(root.resolve("buildSrc"), surfaces);
        collectFiles(root.resolve(".github/workflows"), surfaces);
        collectFiles(root.resolve("scripts/ci"), surfaces);

        for (Path surface : surfaces) {
            if (!Files.isRegularFile(surface)) continue;
            String content = Files.readString(surface);
            for (String obsolete : REMOVED_REFERENCE_TOKENS) {
                assertFalse(content.contains(obsolete),
                        "Removed utility is still referenced by "
                                + root.relativize(surface) + ": " + obsolete);
            }
        }
    }

    @Test
    void securityWorkflowRetainsWholeTreeManifestEvidence() throws IOException {
        String workflow = Files.readString(
                repositoryRoot().resolve(".github/workflows/security-policy.yml")
        );
        assertTrue(workflow.contains("Checkout exact source head"));
        assertTrue(workflow.contains("build/security/tracked-tree-manifest.json"));
        assertTrue(workflow.contains("villaigence-tracked-tree-manifest"));
    }

    private static void collectFiles(Path directory, List<Path> output) throws IOException {
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> paths = Files.walk(directory)) {
            output.addAll(paths.filter(Files::isRegularFile).sorted().toList());
        }
    }

    private static Path repositoryRoot() {
        return Path.of("..").toAbsolutePath().normalize();
    }
}
