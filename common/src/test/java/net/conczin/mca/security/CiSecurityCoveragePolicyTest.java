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
    void deterministicProviderTestsFailBeforeExpensiveProductionAcceptance() throws IOException {
        String workflow = Files.readString(
                repositoryRoot().resolve(".github/workflows/livingworld-ci.yml")
        );

        String providerStep = "- name: Run common and deterministic mock-provider tests";
        String productionStep = "- name: Stage and execute production server acceptance";
        int providerStart = workflow.indexOf(providerStep);
        int productionStart = workflow.indexOf(productionStep);

        assertTrue(providerStart >= 0, "Primary CI must have an explicit deterministic provider gate");
        assertTrue(productionStart >= 0, "Primary CI production acceptance step is missing");
        assertTrue(
                providerStart < productionStart,
                "Deterministic provider tests must fail before expensive production acceptance"
        );

        int providerEnd = workflow.indexOf("\n      - name:", providerStart + providerStep.length());
        assertTrue(providerEnd > providerStart, "Deterministic provider gate is malformed");
        String providerBlock = workflow.substring(providerStart, providerEnd);
        assertTrue(providerBlock.contains(":common:test"), "Provider gate must execute common tests");
        assertTrue(providerBlock.contains("--no-daemon"), "Provider gate must use the bounded CI Gradle mode");
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
    void branchReleasePublicationRequiresCurrentPushToChangeRequestFile() throws IOException {
        String workflow = Files.readString(
                repositoryRoot().resolve(".github/workflows/livingworld-release.yml")
        );

        assertTrue(
                workflow.contains("PUSH_BEFORE_SHA: ${{ github.event.before }}"),
                "Release metadata must receive the exact pre-push commit"
        );

        String condition = "elif [[ \"${GITHUB_EVENT_NAME}\" == 'push'"
                + " && \"${GITHUB_REF_TYPE}\" == 'branch' ]]; then";
        int branchStart = workflow.indexOf(condition);
        int pullRequestStart = workflow.indexOf(
                "elif [[ \"${GITHUB_EVENT_NAME}\" == 'pull_request' ]]; then",
                branchStart
        );
        assertTrue(branchStart >= 0, "Release-request branch-push block is missing");
        assertTrue(pullRequestStart > branchStart, "Release-request branch-push boundary is malformed");

        String branchBlock = workflow.substring(branchStart, pullRequestStart);
        int diffCheck = branchBlock.indexOf(
                "git diff --quiet \"${PUSH_BEFORE_SHA}\" HEAD -- \"${request_file}\""
        );
        int publication = branchBlock.indexOf("publish_release='true'");
        assertTrue(diffCheck >= 0, "Branch publication must diff NEXT_RELEASE.txt for the current push");
        assertTrue(publication > diffCheck, "Publication authorization must occur only after the request-file diff");
        assertTrue(
                branchBlock.contains("0000000000000000000000000000000000000000"),
                "Branch creation with no previous commit must fail closed"
        );
    }

    @Test
    void pullRequestReleaseValidationRunsOnlyWhenRequestFileChanges() throws IOException {
        String workflow = Files.readString(
                repositoryRoot().resolve(".github/workflows/livingworld-release.yml")
        );

        assertFalse(
                workflow.contains("pull_request' && -s \"${request_file}\""),
                "A historical non-empty release request must not affect every pull request"
        );

        String pullRequestCondition =
                "elif [[ \"${GITHUB_EVENT_NAME}\" == 'pull_request' ]]; then";
        int pullRequestStart = workflow.indexOf(pullRequestCondition);
        assertTrue(pullRequestStart >= 0, "Release-request pull-request validation branch is missing");

        int pullRequestEnd = workflow.indexOf("\n          fi", pullRequestStart);
        assertTrue(pullRequestEnd > pullRequestStart, "Release-request pull-request validation branch is malformed");
        String pullRequestBlock = workflow.substring(pullRequestStart, pullRequestEnd);

        int diffCheck = pullRequestBlock.indexOf("if ! git diff --quiet");
        int releaseTagRead = pullRequestBlock.indexOf("release_tag=\"$(sed");
        assertTrue(
                pullRequestBlock.contains(
                        "+refs/heads/${GITHUB_BASE_REF}:refs/remotes/origin/${GITHUB_BASE_REF}"
                ),
                "Pull-request validation must fetch the exact current base branch"
        );
        assertTrue(diffCheck >= 0, "Pull requests must diff NEXT_RELEASE.txt against their base branch");
        assertTrue(
                pullRequestBlock.contains("-- \"${request_file}\"; then"),
                "Pull-request diff must be scoped to NEXT_RELEASE.txt"
        );
        assertTrue(releaseTagRead > diffCheck, "Release metadata may be read only after detecting a request-file change");
        assertTrue(
                pullRequestBlock.contains("release_mode='request-validation'"),
                "A changed request file must use the non-publishing validation mode"
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
