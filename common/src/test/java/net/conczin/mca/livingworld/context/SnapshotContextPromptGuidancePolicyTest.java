package net.conczin.mca.livingworld.context;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotContextPromptGuidancePolicyTest {
    @Test
    void dialogueGuidanceSitsAfterDescriptivePersonalitySocialAndBeforeLoreMemoryLayers() {
        String prompt = SnapshotContextPromptPolicy.compose(
                List.of("Observed weather: rain."),
                List.of("Current NPC personality: friendly."),
                List.of("Dialogue style preference: warm and welcoming."),
                List.of("Server-authored world lore:\nUsually sunny."),
                List.of("BELIEF | provenance=PLAYER_TOLD | confidence=100 | statement=\"It is sunny.\""),
                List.of("DISAGREEMENT | first=\"Gate open\" | second=\"Gate closed\""),
                List.of("VERIFIED | provenance=SYSTEM_OBSERVED | type=OBSERVATION | confidence=100 | summary=\"Yesterday was sunny.\"")
        );

        int fact = prompt.indexOf("Observed weather: rain.");
        int descriptive = prompt.indexOf("Current NPC personality: friendly.");
        int guidance = prompt.indexOf("Dialogue style preference: warm and welcoming.");
        int lore = prompt.indexOf("Usually sunny.");
        int semantic = prompt.indexOf("It is sunny.");
        int disagreement = prompt.indexOf("Gate open");
        int episodic = prompt.indexOf("Yesterday was sunny.");

        assertTrue(fact >= 0);
        assertTrue(descriptive > fact);
        assertTrue(guidance > descriptive);
        assertTrue(lore > guidance);
        assertTrue(semantic > lore);
        assertTrue(disagreement > semantic);
        assertTrue(episodic > disagreement);
        assertTrue(prompt.contains("Treat these facts as authoritative for this turn"));
    }

    @Test
    void emptyGuidancePreservesExistingSixLayerCompositionExactly() {
        List<String> facts = List.of("Observed weather: rain.");
        List<String> descriptive = List.of("Current NPC personality: friendly.");
        List<String> lore = List.of("Server-authored world lore:\nUsually sunny.");
        List<String> semantic = List.of("BELIEF | provenance=PLAYER_TOLD | confidence=100 | statement=\"It is sunny.\"");
        List<String> disagreement = List.of("DISAGREEMENT | first=\"Gate open\" | second=\"Gate closed\"");
        List<String> episodic = List.of("VERIFIED | provenance=SYSTEM_OBSERVED | type=OBSERVATION | confidence=100 | summary=\"Yesterday was sunny.\"");

        String existing = SnapshotContextPromptPolicy.compose(
                facts, descriptive, lore, semantic, disagreement, episodic
        );
        String withEmptyGuidance = SnapshotContextPromptPolicy.compose(
                facts, descriptive, List.of(), lore, semantic, disagreement, episodic
        );

        assertEquals(existing, withEmptyGuidance);
    }
}
