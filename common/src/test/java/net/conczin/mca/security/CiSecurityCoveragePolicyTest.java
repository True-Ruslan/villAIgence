package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void releaseWritePermissionRemainsIsolatedToValidatedReleaseJob() throws IOException {
        String workflow = Files.readString(
                repositoryRoot().resolve(".github/workflows/livingworld-release.yml")
        );

        assertTrue(
                workflow.contains("permissions:\n  contents: read"),
                "Release workflow must default to read-only contents permission"
        );

        String releaseJobBoundary = "github-release:\n"
                + "    needs: build-and-package\n"
                + "    if: needs.build-and-package.outputs.publish_release == 'true'\n"
                + "    runs-on: ubuntu-latest\n"
                + "    permissions:\n"
                + "      contents: write";
        assertTrue(
                workflow.contains(releaseJobBoundary),
                "Contents write permission must remain job-scoped behind the validated publish output"
        );
        assertEquals(
                1L,
                workflow.lines().filter(line -> line.trim().equals("contents: write")).count(),
                "Exactly one release job may receive contents write permission"
        );
        assertFalse(
                workflow.startsWith("permissions:\n  contents: write"),
                "Release workflow must never grant top-level contents write"
        );

        int releaseJobStart = workflow.indexOf("  github-release:");
        assertTrue(releaseJobStart >= 0, "Dedicated release job is missing");
        assertFalse(
                workflow.substring(0, releaseJobStart).contains("contents: write"),
                "Build and acceptance work must remain read-only"
        );
    }

    @Test
    void pullRequestReleaseValidationCannotPublishOrCreateTag() throws IOException {
        String workflow = Files.readString(
                repositoryRoot().resolve(".github/workflows/livingworld-release.yml")
        );

        String pullRequestCondition = "elif [[ \"${GITHUB_EVENT_NAME}\" == 'pull_request'"
                + " && -s \"${request_file}\" ]]; then";
        int pullRequestStart = workflow.indexOf(pullRequestCondition);
        assertTrue(pullRequestStart >= 0, "Release-request pull-request validation branch is missing");

        int pullRequestEnd = workflow.indexOf("\n          fi", pullRequestStart);
        assertTrue(pullRequestEnd > pullRequestStart, "Release-request pull-request validation branch is malformed");
        String pullRequestBlock = workflow.substring(pullRequestStart, pullRequestEnd);

        assertTrue(
                pullRequestBlock.contains("release_mode='request-validation'"),
                "Pull requests must use the non-publishing release validation mode"
        );
        assertFalse(
                pullRequestBlock.contains("publish_release='true'"),
                "Pull requests must never authorize release publication"
        );
        assertTrue(
                workflow.contains("if: needs.build-and-package.outputs.publish_release == 'true'"),
                "Release job must depend on the validated publish output"
        );
    }

    private static Path repositoryRoot() {
        return Path.of("..").toAbsolutePath().normalize();
    }
}
