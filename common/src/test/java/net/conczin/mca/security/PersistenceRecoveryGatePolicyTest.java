package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceRecoveryGatePolicyTest {
    private static final String MATRIX =
            "python3 scripts/ci/persistence_recovery_acceptance.py";
    private static final String CONTRACT =
            "python3 scripts/ci/test_persistence_recovery_acceptance.py";
    private static final String REPORT = "persistence-recovery-report.json";

    @Test
    void nightlyAndReleaseGatesExecuteTheSameRecoveryMatrix() throws IOException {
        String nightly = workflow("livingworld-nightly.yml");
        String release = workflow("livingworld-release.yml");

        for (String required : new String[]{
                MATRIX,
                CONTRACT,
                REPORT,
                "VAI-PERSIST-003",
                "len(cases) != 6"
        }) {
            assertTrue(
                    nightly.contains(required),
                    "Nightly recovery gate is missing: " + required
            );
            assertTrue(
                    release.contains(required),
                    "Release recovery gate is missing: " + required
            );
        }
    }

    @Test
    void releaseDryRunTriggersForRecoveryImplementationChanges() throws IOException {
        String release = workflow("livingworld-release.yml");

        for (String requiredPath : new String[]{
                "common/src/main/java/net/conczin/mca/livingworld/**",
                "common/src/test/java/net/conczin/mca/livingworld/**",
                "scripts/ci/persistence_recovery_acceptance.py",
                "scripts/ci/test_persistence_recovery_acceptance.py",
                "fabric/src/productionAcceptanceFixture/**"
        }) {
            assertTrue(
                    release.contains("- '" + requiredPath + "'"),
                    "Release path filter is missing: " + requiredPath
            );
        }
    }

    @Test
    void recoveryEvidenceIsUploadedEvenWhenTheMatrixFails() throws IOException {
        String nightly = workflow("livingworld-nightly.yml");
        String release = workflow("livingworld-release.yml");

        assertTrue(nightly.contains("Upload persistence recovery evidence"));
        assertTrue(release.contains("Upload persistence recovery evidence"));
        assertTrue(nightly.contains("if: always()"));
        assertTrue(release.contains("if: always()"));
    }

    private static String workflow(String name) throws IOException {
        return Files.readString(
                Path.of("..", ".github", "workflows", name)
                        .toAbsolutePath()
                        .normalize()
        );
    }
}
