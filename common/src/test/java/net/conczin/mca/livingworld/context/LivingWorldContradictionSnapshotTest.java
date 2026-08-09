package net.conczin.mca.livingworld.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingWorldContradictionSnapshotTest {
    @TempDir
    Path tempDir;

    @Test
    void canonicalSnapshotDefensivelyCopiesContradictionContext() {
        List<String> contradictions = new ArrayList<>(List.of("DISAGREEMENT one"));
        LivingWorldContextSnapshot snapshot = new LivingWorldContextSnapshot(
                id(90), id(1), "Player", "Villager",
                List.of("base"),
                List.of("fact"),
                List.of("lore"),
                List.of("episode"),
                List.of("semantic"),
                contradictions,
                List.of(),
                42L,
                100L,
                tempDir,
                false,
                false,
                "en"
        );

        contradictions.add("DISAGREEMENT two");

        assertEquals(List.of("DISAGREEMENT one"), snapshot.contradictionContext());
    }

    @Test
    void olderSnapshotConstructorsDefaultContradictionContextToEmpty() {
        LivingWorldContextSnapshot existingFullSignature = new LivingWorldContextSnapshot(
                id(90), id(1), "Player", "Villager",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                42L, 100L, tempDir, false, false, "en"
        );

        assertTrue(existingFullSignature.contradictionContext().isEmpty());
    }

    @Test
    void captureLoadsDedicatedContextOnlyThroughMemory2SnapshotPath() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java"));
        String compact = source.replaceAll("\\s+", " ");

        assertTrue(source.contains("SemanticContradictionContextProvider"));
        assertTrue(source.contains("loadContradictionContext"));
        assertTrue(compact.contains("List<String> contradictionContext = loadContradictionContext( livingWorld, worldRoot, villager.getUUID(), player.getUUID() )"));
        assertTrue(source.contains("if (!config.memory2Enabled) return List.of();"));
        assertTrue(source.contains("Unable to load bounded Semantic contradiction context"));
        assertFalse(source.contains("SemanticContradictionLifecycle.record"));
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
