package net.conczin.mca.livingworld.release;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseConvergenceContractPolicyTest {
    private static final String CONTRACT = "docs/releases/0.3.0-convergence.json";

    @Test
    void repositoryCarriesMachineReadable03ConvergenceContractAndValidator() throws IOException {
        Path root = repositoryRoot();
        Path contract = root.resolve(CONTRACT);
        Path validator = root.resolve("scripts/ci/release_convergence.py");
        Path validatorTests = root.resolve("scripts/ci/test_release_convergence.py");

        assertTrue(Files.isRegularFile(contract), "0.3 convergence contract must exist");
        assertTrue(Files.isRegularFile(validator), "release convergence validator must exist");
        assertTrue(Files.isRegularFile(validatorTests), "release convergence validator tests must exist");

        String json = Files.readString(contract);
        assertTrue(json.contains("\"candidateTag\": \"0.3.0+1.21.1\""));
        assertTrue(json.contains("\"tag\": \"0.2.0+1.21.1\""));
        assertTrue(json.contains("\"commit\": \"e426f588efefa6aa48a6e536c4a998421bbda241\""));
        assertTrue(json.contains("\"publicationTrigger\": \"docs/releases/NEXT_RELEASE.txt\""));
    }

    @Test
    void ciAndReleaseDryRunExecuteConvergenceValidationThroughCanonicalHarness() throws IOException {
        Path root = repositoryRoot();
        String ci = Files.readString(root.resolve(".github/workflows/livingworld-ci.yml"));
        String release = Files.readString(root.resolve(".github/workflows/livingworld-release.yml"));
        String canonicalRunner = Files.readString(root.resolve("scripts/ci/test_select_acceptance_suites.py"));
        String focusedTests = Files.readString(root.resolve("scripts/ci/test_release_convergence.py"));

        String canonicalCommand = "python3 scripts/ci/test_select_acceptance_suites.py";
        assertTrue(ci.contains(canonicalCommand));
        assertTrue(release.contains(canonicalCommand));
        assertTrue(canonicalRunner.contains(
                "from test_release_convergence import ReleaseConvergenceValidatorTest"));
        assertTrue(focusedTests.contains("GITHUB_WORKFLOW"));
        assertTrue(focusedTests.contains("VillAIgence GitHub Release"));
        assertTrue(focusedTests.contains("RELEASE_VERSION"));
        assertTrue(focusedTests.contains("check_history=release_workflow"));
        assertTrue(focusedTests.contains("history_ref=resolve_history_ref("));
        assertTrue(focusedTests.contains("event_name=os.environ.get(\"GITHUB_EVENT_NAME\", \"\")"));
        assertTrue(focusedTests.contains("base_ref=os.environ.get(\"GITHUB_BASE_REF\", \"\")"));
    }

    @Test
    void releaseRequestFileRemainsASeparatePublicationTrigger() throws IOException {
        Path root = repositoryRoot();
        String release = Files.readString(root.resolve(".github/workflows/livingworld-release.yml"));
        String request = Files.readString(root.resolve("docs/releases/NEXT_RELEASE.txt")).trim();

        assertEquals("0.2.0+1.21.1", request,
                "convergence must not arm the 0.3 publication trigger");
        assertTrue(release.contains("request_file='docs/releases/NEXT_RELEASE.txt'"));
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.isRegularFile(current.resolve("gradlew"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.isRegularFile(parent.resolve("gradlew"))) {
            return parent;
        }
        throw new IllegalStateException("Unable to locate repository root from " + current);
    }
}
