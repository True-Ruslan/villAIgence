package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyChainPolicyTest {
    private static final Pattern ACTION_REFERENCE = Pattern.compile("^\\s*-?\\s*uses:\\s*([^#\\s]+)");
    private static final Pattern IMMUTABLE_ACTION = Pattern.compile("^[^@\\s]+@[0-9a-fA-F]{40}$");
    private static final String GRADLE_9_6_1_BIN_SHA256 =
            "9c0f9fb38f9ee2af9fd4b26feb3a7e4c14a1660edbf9933a14b8f14dde1f0eb5";

    @Test
    void fabricLoomUsesAStableRelease() throws IOException {
        String properties = Files.readString(repositoryRoot().resolve("gradle.properties"));
        String loomLine = properties.lines()
                .filter(line -> line.startsWith("loom_version="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("loom_version is missing"));

        assertFalse(
                loomLine.toUpperCase(Locale.ROOT).contains("SNAPSHOT"),
                "Fabric Loom must use a stable release: " + loomLine
        );
    }

    @Test
    void gradleWrapperDistributionIsChecksumVerified() throws IOException {
        String properties = Files.readString(
                repositoryRoot().resolve("gradle/wrapper/gradle-wrapper.properties")
        );

        assertTrue(
                properties.contains("distributionSha256Sum=" + GRADLE_9_6_1_BIN_SHA256),
                "Gradle 9.6.1 wrapper must pin the official bin distribution SHA-256"
        );
    }

    @Test
    void everyExternalGithubActionUsesAnImmutableCommitSha() throws IOException {
        Path workflows = repositoryRoot().resolve(".github/workflows");
        assertTrue(Files.isDirectory(workflows), workflows.toAbsolutePath().toString());

        List<String> mutableReferences = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(workflows)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                int lineNumber = 0;
                for (String line : Files.readAllLines(path)) {
                    lineNumber++;
                    Matcher matcher = ACTION_REFERENCE.matcher(line);
                    if (!matcher.find()) continue;
                    String reference = matcher.group(1);
                    if (reference.startsWith("./") || IMMUTABLE_ACTION.matcher(reference).matches()) continue;
                    mutableReferences.add(repositoryRoot().relativize(path)
                            + ":" + lineNumber + " -> " + reference);
                }
            }
        }

        assertTrue(
                mutableReferences.isEmpty(),
                "GitHub Actions must use full commit SHAs:\n" + String.join("\n", mutableReferences)
        );
    }

    private static Path repositoryRoot() {
        return Path.of("..").toAbsolutePath().normalize();
    }
}
