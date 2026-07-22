package net.conczin.mca.livingworld.context;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LivingWorldContextSnapshotMemoryTest {
    @Test
    void memoryLayersAreSeparateAndDefensivelyCopied() {
        List<String> contextLines = new ArrayList<>(List.of("personality"));
        List<String> worldFacts = new ArrayList<>(List.of("Observed biome: plains."));
        List<String> memoryContext = new ArrayList<>(List.of(
                "BELIEF | provenance=PLAYER_TOLD | type=DIALOGUE | confidence=50 | summary=\"A claim\""
        ));
        List<String> semanticMemoryContext = new ArrayList<>(List.of(
                "BELIEF | provenance=PLAYER_TOLD | confidence=80 | statement=\"A semantic claim\""
        ));
        List<LivingWorldContextSnapshot.ActionDescriptor> actions = new ArrayList<>(List.of(
                new LivingWorldContextSnapshot.ActionDescriptor("follow-player", "Follow the player")
        ));

        LivingWorldContextSnapshot snapshot = new LivingWorldContextSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Player",
                "Villager",
                contextLines,
                worldFacts,
                memoryContext,
                semanticMemoryContext,
                actions,
                123L,
                456L,
                Path.of("."),
                false,
                false,
                "en"
        );

        contextLines.clear();
        worldFacts.clear();
        memoryContext.clear();
        semanticMemoryContext.clear();
        actions.clear();

        assertEquals(List.of("personality"), snapshot.contextLines());
        assertEquals(List.of("Observed biome: plains."), snapshot.worldFacts());
        assertEquals(1, snapshot.memoryContext().size());
        assertEquals(1, snapshot.semanticMemoryContext().size());
        assertEquals(1, snapshot.availableActions().size());
        assertFalse(snapshot.worldFacts().contains(snapshot.memoryContext().getFirst()));
        assertFalse(snapshot.worldFacts().contains(snapshot.semanticMemoryContext().getFirst()));
        assertFalse(snapshot.contextLines().contains(snapshot.memoryContext().getFirst()));
        assertFalse(snapshot.contextLines().contains(snapshot.semanticMemoryContext().getFirst()));
    }
}
