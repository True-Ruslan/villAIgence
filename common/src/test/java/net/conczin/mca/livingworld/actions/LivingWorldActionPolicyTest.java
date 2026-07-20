package net.conczin.mca.livingworld.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingWorldActionPolicyTest {
    @Test
    void configuredLivingWorldUsesItsSafeActionSwitch() {
        assertTrue(LivingWorldActionPolicy.shouldExposeTools(true, true, false));
        assertFalse(LivingWorldActionPolicy.shouldExposeTools(true, false, true));
    }

    @Test
    void legacyMcaKeepsItsExistingToolSwitch() {
        assertTrue(LivingWorldActionPolicy.shouldExposeTools(false, false, true));
        assertFalse(LivingWorldActionPolicy.shouldExposeTools(false, true, false));
    }
}
