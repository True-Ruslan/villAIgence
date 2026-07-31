package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CiSecurityCoveragePolicyTest {
    @Test
    void primaryCiBuildsEverySupportedLoaderAndRunsRepositorySecurityPolicy() throws IOException {
        Path root = repositoryRoot();
        String workflow = Files.readString(root.resolve(".github/workflows/livingworld-ci.yml"));

        assertTrue(workflow.contains(":common:test"), "Primary CI must retain common tests");
        assertTrue(workflow.contains(":fabric:build"), "Primary CI must build Fabric");
        assertTrue(workflow.contains(":neoforge:build"), "Primary CI must build NeoForge");
        assertTrue(
                workflow.contains("python3 scripts/ci/repository_security_policy.py --check"),
                "Primary CI must run the deterministic repository security policy"
        );
    }

    @Test
    void repositorySecurityPolicyAndApprovedInventoryAreCommitted() {
        Path root = repositoryRoot();
        assertTrue(
                Files.isRegularFile(root.resolve("scripts/ci/repository_security_policy.py")),
                "Repository security policy runner is missing"
        );
        assertTrue(
                Files.isRegularFile(root.resolve("docs/security/APPROVED_SCRIPT_INVENTORY.json")),
                "Approved recursive script inventory is missing"
        );
    }

    @Test
    void releaseWritePermissionRemainsIsolatedToReleaseJob() throws IOException {
        String workflow = Files.readString(
                repositoryRoot().resolve(".github/workflows/livingworld-release.yml")
        );

        assertTrue(
                workflow.contains("permissions:\n  contents: read"),
                "Release workflow must default to read-only contents permission"
        );
        assertTrue(
                workflow.contains("github-release:\n    needs: build-and-package"),
                "Dedicated release job is missing"
        );
        assertTrue(
                workflow.contains("github-release:\n    needs: build-and-package\n    if: github.event_name == 'push'\n    runs-on: ubuntu-latest\n    permissions:\n      contents: write"),
                "Contents write permission must remain job-scoped to the tag-only release job"
        );
        assertFalse(
                workflow.startsWith("permissions:\n  contents: write"),
                "Release workflow must never grant top-level contents write"
        );
    }

    private static Path repositoryRoot() {
        return Path.of("..").toAbsolutePath().normalize();
    }
}
