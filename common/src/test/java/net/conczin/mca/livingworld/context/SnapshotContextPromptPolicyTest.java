package net.conczin.mca.livingworld.context;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotContextPromptPolicyTest {
    @Test
    void observedFactsPrecedeOperatorLoreAndWinConflicts() {
        String prompt = SnapshotContextPromptPolicy.compose(
                List.of("Observed weather: rain."),
                List.of("Server-authored world lore:\nThe kingdom is usually sunny.")
        );

        int observed = prompt.indexOf("Observed factual context");
        int lore = prompt.indexOf("Server-authored lore supplied by the server operator");

        assertTrue(observed >= 0);
        assertTrue(lore > observed);
        assertTrue(prompt.contains("Treat these facts as authoritative for this turn"));
        assertTrue(prompt.contains("current observed facts take precedence"));
        assertTrue(prompt.contains("- Observed weather: rain."));
        assertTrue(prompt.contains("Server-authored world lore:\nThe kingdom is usually sunny."));
    }

    @Test
    void emptySectionsProduceNoPromptText() {
        assertEquals("", SnapshotContextPromptPolicy.compose(List.of(), List.of()));
        assertEquals("", SnapshotContextPromptPolicy.compose(null, null));
    }

    @Test
    void blankEntriesAreIgnoredWithoutRemovingMultilineLore() {
        String prompt = SnapshotContextPromptPolicy.compose(
                List.of("", "Observed biome: plains."),
                List.of("   ", "Server-authored villager lore:\nRetired cartographer.")
        );

        assertFalse(prompt.contains("- \n"));
        assertTrue(prompt.contains("- Observed biome: plains."));
        assertTrue(prompt.contains("Server-authored villager lore:\nRetired cartographer."));
    }

    @Test
    void snapshotDefensivelyCopiesOperatorAuthoredContext() {
        List<String> lore = new ArrayList<>(List.of("Server-authored world lore:\nOriginal"));
        LivingWorldContextSnapshot snapshot = new LivingWorldContextSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Player",
                "Villager",
                List.of(),
                List.of(),
                lore,
                List.of(),
                List.of(),
                List.of(),
                42L,
                100L,
                Path.of("world"),
                false,
                false,
                "ru_ru"
        );

        lore.set(0, "mutated");

        assertEquals(List.of("Server-authored world lore:\nOriginal"), snapshot.operatorAuthoredContext());
    }
}
