package net.conczin.mca.livingworld.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcVoiceMoodResolverTest {
    @Test
    void resolvesMoodFromAuthoritativeStateWithSafetyPriority() {
        assertEquals(NpcVoiceMood.AFRAID, NpcVoiceMoodResolver.resolve(true, 1.0, 80, 0, 80));
        assertEquals(NpcVoiceMood.AFRAID, NpcVoiceMoodResolver.resolve(false, 1.0, 80, 60, 80));
        assertEquals(NpcVoiceMood.SAD, NpcVoiceMoodResolver.resolve(false, 0.20, 80, 0, 80));
        assertEquals(NpcVoiceMood.ANGRY, NpcVoiceMoodResolver.resolve(false, 1.0, -60, 0, -20));
        assertEquals(NpcVoiceMood.HAPPY, NpcVoiceMoodResolver.resolve(false, 1.0, 50, 0, 60));
        assertEquals(NpcVoiceMood.TIRED, NpcVoiceMoodResolver.resolve(false, 0.55, 0, 0, 0));
        assertEquals(NpcVoiceMood.NEUTRAL, NpcVoiceMoodResolver.resolve(false, 1.0, 0, 0, 0));
    }

    @Test
    void styleKeepsAgeGuidanceSeparateFromVoiceIdentity() {
        TtsVoiceStyle childHappy = NpcVoiceMoodResolver.style(NpcVoiceMood.HAPPY, NpcVoiceAgeGroup.CHILD);
        TtsVoiceStyle teenAngry = NpcVoiceMoodResolver.style(NpcVoiceMood.ANGRY, NpcVoiceAgeGroup.TEEN);
        TtsVoiceStyle adultNeutral = NpcVoiceMoodResolver.style(NpcVoiceMood.NEUTRAL, NpcVoiceAgeGroup.ADULT);

        assertTrue(childHappy.instructions().contains("youthful"));
        assertTrue(childHappy.instructions().contains("warm"));
        assertTrue(teenAngry.instructions().contains("teenager"));
        assertTrue(teenAngry.instructions().contains("irritation"));
        assertFalse(adultNeutral.instructions().isBlank());
        assertTrue(childHappy.speed() >= 0.25 && childHappy.speed() <= 4.0);
        assertTrue(teenAngry.speed() >= 0.25 && teenAngry.speed() <= 4.0);
    }
}
