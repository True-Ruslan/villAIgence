package net.conczin.mca.livingworld.context;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotContradictionPromptPolicyTest {
    @Test
    void disagreementLayerRendersAfterSemanticAndBeforeEpisodicHistory() {
        String prompt = SnapshotContextPromptPolicy.compose(
                List.of("Observed gate: open."),
                List.of("Operator background"),
                List.of("BELIEF | provenance=NPC_TOLD | confidence=60 | statement=\"gate closed\""),
                List.of("DISAGREEMENT | first={FACT ...} | second={BELIEF ...}"),
                List.of("DIALOGUE | historical turn")
        );

        int fact = prompt.indexOf("Observed gate: open.");
        int lore = prompt.indexOf("Operator background");
        int semantic = prompt.indexOf("gate closed");
        int disagreement = prompt.indexOf("DISAGREEMENT | first=");
        int episodic = prompt.indexOf("DIALOGUE | historical turn");

        assertTrue(fact >= 0 && fact < lore);
        assertTrue(lore < semantic);
        assertTrue(semantic < disagreement);
        assertTrue(disagreement < episodic);
        assertEquals(1, occurrences(prompt, "NPC remembered disagreements."));
        assertTrue(prompt.contains("Current observed factual context wins on conflict."));
        assertTrue(prompt.contains("does not decide which claim is true"));
        assertFalse(prompt.contains("winner="));
    }

    @Test
    void emptyDisagreementLayerPreservesExistingFourLayerPromptExactly() {
        List<String> facts = List.of("Observed gate: open.");
        List<String> lore = List.of("Operator background");
        List<String> semantic = List.of("BELIEF | provenance=NPC_TOLD | confidence=60 | statement=\"gate closed\"");
        List<String> episodic = List.of("DIALOGUE | historical turn");

        assertEquals(
                SnapshotContextPromptPolicy.compose(facts, lore, semantic, episodic),
                SnapshotContextPromptPolicy.compose(facts, lore, semantic, List.of(), episodic)
        );
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
