package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticBeliefExtractionPromptTest {
    @Test
    void disabledExtractionAddsNoPromptInstruction() {
        assertTrue(SemanticBeliefExtractionPrompt.instruction(false, 3).isEmpty());
        assertFalse(SemanticBeliefExtractionPrompt.requiresStructuredResponse(false, false, false));
    }

    @Test
    void enabledExtractionForcesStructuredResponseEvenWithoutActionsOrRelationships() {
        assertTrue(SemanticBeliefExtractionPrompt.requiresStructuredResponse(false, false, true));
        assertTrue(SemanticBeliefExtractionPrompt.requiresStructuredResponse(true, false, false));
        assertTrue(SemanticBeliefExtractionPrompt.requiresStructuredResponse(false, true, false));
    }

    @Test
    void instructionRestrictsCandidatesToLatestExplicitPlayerClaims() {
        String instruction = SemanticBeliefExtractionPrompt.instruction(true, 3);

        assertTrue(instruction.contains("beliefCandidates"));
        assertTrue(instruction.contains("latest player message"));
        assertTrue(instruction.contains("explicitly asserted"));
        assertTrue(instruction.contains("at most 3"));
        assertTrue(instruction.contains("[]"));
        assertTrue(instruction.contains("non-authoritative BELIEF"));
        assertTrue(instruction.contains("Never include claims originating only from the NPC reply"));
    }

    @Test
    void configuredLimitIsNormalizedBeforeItEntersPrompt() {
        assertTrue(SemanticBeliefExtractionPrompt.instruction(true, 0).contains("at most 3"));
        assertTrue(SemanticBeliefExtractionPrompt.instruction(true, 99).contains("at most 8"));
    }
}
