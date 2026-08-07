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

class LegacyConversationMemoryClosurePolicyTest {
    private static final String LEGACY_STORE = "memory.json";

    @Test
    void legacyConversationStoreTypesAreRemoved() {
        Path root = repositoryRoot();
        assertFalse(Files.exists(root.resolve(
                "common/src/main/java/net/conczin/mca/livingworld/memory/ConversationMemoryStore.java"
        )));
        assertFalse(Files.exists(root.resolve(
                "common/src/main/java/net/conczin/mca/livingworld/memory/MemoryMessage.java"
        )));
    }

    @Test
    void compatibilityFacadeCanOnlyReadMemory2AndCannotPersistSeparately() throws IOException {
        String source = Files.readString(repositoryRoot().resolve(
                "common/src/main/java/net/conczin/mca/livingworld/memory/PersistentChatMemory.java"
        ));

        assertTrue(source.contains("Memory2DialogueHistory.load"));
        assertFalse(source.contains("ConversationMemoryStore"));
        assertFalse(source.contains(LEGACY_STORE));
        assertFalse(source.contains("writeAtomic"));
        assertFalse(source.contains("appendExchange"));
    }

    @Test
    void activeProductionAndAcceptanceSurfacesCannotReferenceLegacyStore() throws IOException {
        Path root = repositoryRoot();
        List<Path> surfaces = new ArrayList<>();
        collectJava(root.resolve("common/src/main/java"), surfaces);
        collectJava(root.resolve("fabric/src/productionAcceptanceFixture/java"), surfaces);
        surfaces.add(root.resolve("scripts/ci/production_server_acceptance.py"));
        surfaces.add(root.resolve("scripts/ci/persistence_recovery_acceptance.py"));
        surfaces.add(root.resolve("scripts/ci/test_persistence_recovery_acceptance.py"));

        for (Path surface : surfaces) {
            if (!Files.isRegularFile(surface)) continue;
            String content = Files.readString(surface);
            assertFalse(content.contains(LEGACY_STORE),
                    "Legacy persistent store is still referenced by " + root.relativize(surface));
        }
    }

    private static void collectJava(Path directory, List<Path> output) throws IOException {
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> paths = Files.walk(directory)) {
            output.addAll(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList());
        }
    }

    private static Path repositoryRoot() {
        return Path.of("..").toAbsolutePath().normalize();
    }
}
