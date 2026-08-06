package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionAcceptanceGateParityPolicyTest {
    private static final String STRICT_ENTRYPOINT =
            "python3 scripts/ci/production_server_acceptance_strict.py";
    private static final String LIFECYCLE_TEST =
            "python3 scripts/ci/test_production_lifecycle_acceptance.py";
    private static final String LIFECYCLE_VERIFIER =
            "python3 scripts/ci/production_lifecycle_acceptance.py";
    private static final String LIFECYCLE_REPORT = "lifecycle-report.json";

    @Test
    void pullRequestAndReleaseGatesUseTheSameStrictLifecycleOracles()
            throws IOException {
        String pullRequestWorkflow = workflow("livingworld-ci.yml");
        String releaseWorkflow = workflow("livingworld-release.yml");

        for (String required : new String[]{
                STRICT_ENTRYPOINT,
                LIFECYCLE_TEST,
                LIFECYCLE_VERIFIER,
                LIFECYCLE_REPORT,
                "VILLAIGENCE_PRODUCTION_FIXTURE_READY"
        }) {
            assertTrue(
                    pullRequestWorkflow.contains(required),
                    "Pull-request production gate is missing: " + required
            );
            assertTrue(
                    releaseWorkflow.contains(required),
                    "Release production gate is missing: " + required
            );
        }
    }

    @Test
    void releaseWorkflowTriggersWhenStrictLifecycleSourcesChange()
            throws IOException {
        String releaseWorkflow = workflow("livingworld-release.yml");

        for (String path : new String[]{
                "scripts/ci/production_lifecycle_acceptance.py",
                "scripts/ci/production_server_acceptance_strict.py",
                "scripts/ci/test_production_lifecycle_acceptance.py"
        }) {
            assertTrue(
                    releaseWorkflow.contains("- '" + path + "'"),
                    "Release workflow path filter is missing: " + path
            );
        }
    }

    private static String workflow(String name) throws IOException {
        Path path = Path.of("..", ".github", "workflows", name)
                .toAbsolutePath()
                .normalize();
        return Files.readString(path);
    }
}
