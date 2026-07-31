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
            "9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14";

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

    @Test
    void dependencyVerificationMetadataIsCommitted() throws IOException {
        Path metadata = repositoryRoot().resolve("gradle/verification-metadata.xml");
        assertTrue(Files.isRegularFile(metadata), "Gradle dependency verification metadata is missing");

        String xml = Files.readString(metadata);
        assertTrue(xml.contains("<verification-metadata"));
        assertTrue(xml.contains("<sha256 value="), "Verification metadata must contain SHA-256 checksums");
    }

    @Test
    void onlyLocallyGeneratedLoomArtifactsReceiveChecksumExemptions() throws IOException {
        String xml = Files.readString(repositoryRoot().resolve("gradle/verification-metadata.xml"));
        String layeredMappingsTrust = "<trust group=\"loom\" name=\"mappings\" "
                + "version=\"layered[+]hash[.][0-9]+\" "
                + "file=\"mappings-layered[+]hash[.][0-9]+[.]jar\" regex=\"true\"";
        String remappedJarTrust = "<trust group=\"^remapped[.].+\" file=\".*[.]jar\" regex=\"true\"";
        String mergedMinecraftTrust = "<trust group=\"net[.]minecraft\" "
                + "name=\"minecraft-merged-[0-9a-f]+\" "
                + "version=\"1[.]21[.]1-loom[.]mappings[.].+\" "
                + "file=\"minecraft-merged-[0-9a-f]+-1[.]21[.]1-loom[.]mappings[.].+[.]jar\" "
                + "regex=\"true\"";

        assertTrue(xml.contains(layeredMappingsTrust),
                "Locally generated Loom layered mappings need a narrow trusted-artifact rule");
        assertTrue(xml.contains(remappedJarTrust),
                "Locally remapped Loom JARs need a file-scoped trusted-artifact rule");
        assertTrue(xml.contains(mergedMinecraftTrust),
                "The locally merged Minecraft development JAR needs a coordinate-scoped trusted-artifact rule");

        long trustRuleCount = xml.lines().filter(line -> line.contains("<trust ")).count();
        assertTrue(trustRuleCount == 3,
                "Only the three reviewed Loom-generated artifact classes may bypass checksums");
        assertFalse(xml.contains("<trust group=\"loom\" reason="),
                "The entire Loom group must never be trusted without artifact constraints");
        assertFalse(xml.contains("<trust group=\"net.fabricmc\""),
                "External Fabric artifacts must remain checksum verified");
    }

    @Test
    void buildScriptsCannotDeclareSyntheticRemappedCoordinates() throws IOException {
        List<String> buildFiles = List.of(
                "build.gradle",
                "settings.gradle",
                "gradle.properties",
                "common/build.gradle",
                "fabric/build.gradle",
                "neoforge/build.gradle"
        );

        for (String buildFile : buildFiles) {
            String content = Files.readString(repositoryRoot().resolve(buildFile));
            assertFalse(content.contains("remapped."),
                    buildFile + " must not declare Loom's synthetic remapped dependency coordinates");
        }
    }

    @Test
    void dependencyLockingAndProjectLockfilesAreCommitted() throws IOException {
        String rootBuild = Files.readString(repositoryRoot().resolve("build.gradle"));
        assertTrue(rootBuild.contains("lockAllConfigurations()"), "Dependency locking must be enabled");

        for (String project : List.of("common", "fabric", "neoforge")) {
            Path lockfile = repositoryRoot().resolve(project).resolve("gradle.lockfile");
            assertTrue(Files.isRegularFile(lockfile), project + " dependency lockfile is missing");
            assertFalse(Files.readString(lockfile).isBlank(), project + " dependency lockfile is empty");
        }
    }

    @Test
    void releasePublishesDeterministicDependencyManifest() throws IOException {
        String rootBuild = Files.readString(repositoryRoot().resolve("build.gradle"));
        String releaseWorkflow = Files.readString(
                repositoryRoot().resolve(".github/workflows/livingworld-release.yml")
        );

        assertTrue(rootBuild.contains("tasks.register('dependencyManifest')"));
        assertTrue(rootBuild.contains("common/gradle.lockfile"));
        assertTrue(rootBuild.contains("fabric/gradle.lockfile"));
        assertTrue(rootBuild.contains("neoforge/gradle.lockfile"));
        assertTrue(releaseWorkflow.contains("dependencyManifest"));
        assertTrue(releaseWorkflow.contains("villaigence-dependencies.txt"));
    }

    private static Path repositoryRoot() {
        return Path.of("..").toAbsolutePath().normalize();
    }
}
