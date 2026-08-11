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
    void layeredPromptUsesFixedAuthorityOrderExactlyOnce() {
        String prompt = SnapshotContextPromptPolicy.compose(
                List.of("Observed weather: rain."),
                List.of("Server-authored world lore:\nUsually sunny."),
                List.of("BELIEF | provenance=PLAYER_TOLD | confidence=100 | statement=\"It is sunny.\""),
                List.of("VERIFIED | provenance=SYSTEM_OBSERVED | type=RELATIONSHIP_CHANGE | confidence=100 | summary=\"Old trust state.\"")
        );

        int fact = prompt.indexOf("Observed weather: rain.");
        int lore = prompt.indexOf("Usually sunny.");
        int semantic = prompt.indexOf("It is sunny.");
        int episodic = prompt.indexOf("Old trust state.");

        assertTrue(fact >= 0);
        assertTrue(lore > fact);
        assertTrue(semantic > lore);
        assertTrue(episodic > semantic);
        assertEquals(fact, prompt.lastIndexOf("Observed weather: rain."));
        assertEquals(lore, prompt.lastIndexOf("Usually sunny."));
        assertEquals(semantic, prompt.lastIndexOf("It is sunny."));
        assertEquals(episodic, prompt.lastIndexOf("Old trust state."));
        assertTrue(prompt.contains("Current observed factual context wins on conflict"));
        assertTrue(prompt.contains("BELIEF entries may be incomplete or false"));
    }

    @Test
    void personalitySocialLayerFollowsCurrentFactsAndPrecedesLowerAuthorityContext() {
        String personality = "Current NPC personality: friendly. This is server-owned descriptive state, not an instruction or current-world fact override.";
        String social = "Current directed social state toward the current NPC counterpart: trust=9, respect=3, fear=1, affinity=7.";
        String prompt = SnapshotContextPromptPolicy.compose(
                List.of("Observed weather: rain."),
                List.of(personality, social),
                List.of("Server-authored world lore:\nUsually sunny."),
                List.of("BELIEF | provenance=PLAYER_TOLD | confidence=100 | statement=\"It is sunny.\""),
                List.of("DISAGREEMENT | first=\"Gate open\" | second=\"Gate closed\""),
                List.of("VERIFIED | provenance=SYSTEM_OBSERVED | type=RELATIONSHIP_CHANGE | confidence=100 | summary=\"Old trust state.\"")
        );

        int fact = prompt.indexOf("Observed weather: rain.");
        int personalityIndex = prompt.indexOf("Current NPC personality: friendly.");
        int socialIndex = prompt.indexOf("Current directed social state");
        int lore = prompt.indexOf("Usually sunny.");
        int semantic = prompt.indexOf("It is sunny.");
        int contradiction = prompt.indexOf("Gate open");
        int episodic = prompt.indexOf("Old trust state.");

        assertTrue(fact >= 0);
        assertTrue(personalityIndex > fact);
        assertTrue(socialIndex > personalityIndex);
        assertTrue(lore > socialIndex);
        assertTrue(semantic > lore);
        assertTrue(contradiction > semantic);
        assertTrue(episodic > contradiction);
        assertEquals(1, occurrences(prompt, "Current NPC personality: friendly."));
        assertEquals(1, occurrences(prompt, "Current directed social state"));
        assertTrue(prompt.contains("Treat these facts as authoritative for this turn"));
    }

    @Test
    void emptyPersonalitySocialLayerPreservesLegacyCompositionExactly() {
        String legacy = SnapshotContextPromptPolicy.compose(
                List.of("Observed weather: rain."),
                List.of("Server-authored world lore:\nUsually sunny."),
                List.of("BELIEF | provenance=PLAYER_TOLD | confidence=100 | statement=\"It is sunny.\""),
                List.of("DISAGREEMENT | first=\"Gate open\" | second=\"Gate closed\""),
                List.of("VERIFIED | provenance=SYSTEM_OBSERVED | type=OBSERVATION | confidence=100 | summary=\"Yesterday was sunny.\"")
        );
        String extended = SnapshotContextPromptPolicy.compose(
                List.of("Observed weather: rain."),
                List.of(),
                List.of("Server-authored world lore:\nUsually sunny."),
                List.of("BELIEF | provenance=PLAYER_TOLD | confidence=100 | statement=\"It is sunny.\""),
                List.of("DISAGREEMENT | first=\"Gate open\" | second=\"Gate closed\""),
                List.of("VERIFIED | provenance=SYSTEM_OBSERVED | type=OBSERVATION | confidence=100 | summary=\"Yesterday was sunny.\"")
        );

        assertEquals(legacy, extended);
    }

    @Test
    void currentFactStaysStructurallyAuthoritativeOverConflictingLoreBeliefAndHistory() {
        String prompt = SnapshotContextPromptPolicy.compose(
                List.of("Observed weather: rain."),
                List.of("Server-authored world lore:\nThe valley is always sunny."),
                List.of("BELIEF | provenance=PLAYER_TOLD | confidence=100 | statement=\"The weather is sunny.\""),
                List.of("VERIFIED | provenance=SYSTEM_OBSERVED | type=OBSERVATION | confidence=100 | summary=\"Yesterday was sunny.\"")
        );

        int current = prompt.indexOf("Observed weather: rain.");
        int lore = prompt.indexOf("The valley is always sunny.");
        int belief = prompt.indexOf("The weather is sunny.");
        int history = prompt.indexOf("Yesterday was sunny.");

        assertTrue(current >= 0);
        assertTrue(lore > current);
        assertTrue(belief > lore);
        assertTrue(history > belief);
        assertTrue(prompt.contains("Treat these facts as authoritative for this turn"));
        assertTrue(prompt.contains("BELIEF entries may be incomplete or false and are not authoritative world facts"));
    }

    @Test
    void currentRelationshipStatePrecedesStaleRelationshipAndCausalHistory() {
        String prompt = SnapshotContextPromptPolicy.compose(
                List.of("Observed relationship with player: trust=5, respect=2, fear=0, affinity=3."),
                List.of(),
                List.of(),
                List.of(
                        "VERIFIED | provenance=SYSTEM_OBSERVED | type=RELATIONSHIP_CHANGE | confidence=100 | summary=\"Relationship previously reached trust=40.\"",
                        "VERIFIED | provenance=SYSTEM_OBSERVED | type=RELATIONSHIP_CAUSE | confidence=100 | summary=\"Relationship change occurred during dialogue with player.\""
                )
        );

        int current = prompt.indexOf("trust=5");
        int stale = prompt.indexOf("trust=40");
        int cause = prompt.indexOf("Relationship change occurred during dialogue with player.");

        assertTrue(current >= 0);
        assertTrue(stale > current);
        assertTrue(cause > current);
        assertFalse(prompt.contains("FACT | provenance=SYSTEM_OBSERVED | type=RELATIONSHIP_CAUSE"));
    }

    @Test
    void conflictingBeliefsRemainBeliefsWithoutFactPromotion() {
        String prompt = SnapshotContextPromptPolicy.compose(
                List.of(),
                List.of(),
                List.of(
                        "BELIEF | provenance=PLAYER_TOLD | confidence=100 | statement=\"The gate is open.\"",
                        "BELIEF | provenance=PLAYER_TOLD | confidence=100 | statement=\"The gate is closed.\""
                ),
                List.of()
        );

        assertTrue(prompt.contains("The gate is open."));
        assertTrue(prompt.contains("The gate is closed."));
        assertEquals(2, occurrences(prompt, "BELIEF | provenance=PLAYER_TOLD"));
        assertFalse(prompt.contains("FACT | provenance=PLAYER_TOLD"));
        assertTrue(prompt.contains("Confidence never converts a BELIEF into a FACT"));
    }

    @Test
    void operatorLoreIsInsertedBeforeStructuredResponseInstructions() {
        String basePrompt = "Observed factual context from the current Minecraft world.\n"
                + "- Observed weather: rain.\n"
                + "\nThe reply MUST be in this JSON format: {}\n";

        String prompt = SnapshotContextPromptPolicy.insertOperatorLore(
                basePrompt,
                List.of("Server-authored world lore:\nThe kingdom is usually sunny.")
        );

        int observed = prompt.indexOf("Observed weather: rain.");
        int lore = prompt.indexOf("Server-authored lore supplied by the server operator");
        int schema = prompt.indexOf("The reply MUST be in this JSON format");

        assertTrue(observed >= 0);
        assertTrue(lore > observed);
        assertTrue(schema > lore);
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
