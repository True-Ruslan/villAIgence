package net.conczin.mca.livingworld;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticBeliefExtractionConfigTest {
    @Test
    void defaultsKeepBeliefExtractionExplicitlyDisabledAndBounded() {
        LivingWorldConfig config = new LivingWorldConfig();

        assertFalse(config.semanticBeliefExtractionEnabled);
        assertEquals(3, config.semanticBeliefMaxCandidatesPerTurn);
    }

    @Test
    void versionTwoConfigPreservesExplicitExtractionSettings() {
        LivingWorldConfig config = LivingWorldConfig.parseJson("""
                {
                  "version": 2,
                  "semanticBeliefExtractionEnabled": true,
                  "semanticBeliefMaxCandidatesPerTurn": 6
                }
                """);

        assertTrue(config.semanticBeliefExtractionEnabled);
        assertEquals(6, config.semanticBeliefMaxCandidatesPerTurn);
    }

    @Test
    void invalidCandidateLimitsNormalizeWithoutConfigVersionMigration() {
        LivingWorldConfig omitted = LivingWorldConfig.parseJson("""
                {"version":2}
                """);
        assertFalse(omitted.semanticBeliefExtractionEnabled);
        assertEquals(3, omitted.semanticBeliefMaxCandidatesPerTurn);

        LivingWorldConfig zero = LivingWorldConfig.parseJson("""
                {"version":2,"semanticBeliefMaxCandidatesPerTurn":0}
                """);
        assertEquals(3, zero.semanticBeliefMaxCandidatesPerTurn);

        LivingWorldConfig negative = LivingWorldConfig.parseJson("""
                {"version":2,"semanticBeliefMaxCandidatesPerTurn":-5}
                """);
        assertEquals(3, negative.semanticBeliefMaxCandidatesPerTurn);

        LivingWorldConfig upperBound = LivingWorldConfig.parseJson("""
                {"version":2,"semanticBeliefMaxCandidatesPerTurn":8}
                """);
        assertEquals(8, upperBound.semanticBeliefMaxCandidatesPerTurn);

        LivingWorldConfig tooLarge = LivingWorldConfig.parseJson("""
                {"version":2,"semanticBeliefMaxCandidatesPerTurn":99}
                """);
        assertEquals(8, tooLarge.semanticBeliefMaxCandidatesPerTurn);
    }
}
