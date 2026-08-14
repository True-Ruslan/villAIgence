package net.conczin.mca.livingworld.release;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentReleaseHandoffPolicyTest {
    private static final String RELEASE = "0.3.1+1.21.1";
    private static final String RELEASE_COMMIT = "bc7c68ac2f3a4f761aa3b03a2f5c1fe1201745ab";
    private static final String RELEASE_SHA = "f7f40b920c6f72a0e9af864795f48a0f90479db42a145081f43923b71a95e29f";

    @Test
    void canonicalHandoffTracksPublished031AndPendingInstalledAcceptance() throws IOException {
        Path root = repositoryRoot();
        String state = Files.readString(root.resolve("docs/PROJECT_STATE.md"));
        String roadmap = Files.readString(root.resolve("docs/ROADMAP.md"));

        assertTrue(state.contains("latest official release:            " + RELEASE));
        assertTrue(state.contains("latest release commit:             " + RELEASE_COMMIT));
        assertTrue(state.contains("installed 0.3.1 acceptance"));
        assertTrue(state.contains("VAI-PCM-MULTI-001  PENDING"));
        assertFalse(state.contains("VAI-PCM-MULTI-001  PASS"));

        int installed = roadmap.indexOf("installed 0.3.1 acceptance");
        int knowledge = roadmap.indexOf("0.4 — Knowledge ecosystem and rumors");
        assertTrue(installed >= 0, "roadmap must name installed 0.3.1 acceptance");
        assertTrue(knowledge > installed, "0.4 must remain after installed 0.3.1 acceptance");
    }

    @Test
    void dedicated031InstalledLedgerPreservesExactEvidenceBoundary() throws IOException {
        Path root = repositoryRoot();
        Path ledger = root.resolve("docs/livingworld/VALIDATION_0.3.1_CLEAN_WORLD_INSTALLED.md");
        assertTrue(Files.isRegularFile(ledger), "0.3.1 installed validation ledger must exist");

        String text = Files.readString(ledger);
        assertTrue(text.contains("Status: **PENDING INSTALLED ACCEPTANCE**"));
        assertTrue(text.contains(RELEASE));
        assertTrue(text.contains(RELEASE_COMMIT));
        assertTrue(text.contains(RELEASE_SHA));
        assertTrue(text.contains("VAI-PCM-MULTI-001"));
        assertTrue(text.contains("VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY"));
        assertTrue(text.contains("VAI-CONCUR-004   NOT TESTED / DEFERRED"));
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
