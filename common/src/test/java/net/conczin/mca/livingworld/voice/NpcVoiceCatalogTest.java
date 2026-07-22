package net.conczin.mca.livingworld.voice;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NpcVoiceCatalogTest {
    private static NpcVoiceCatalog catalog() {
        return new NpcVoiceCatalog(new NpcVoiceCatalog.VoicePools(
                List.of("boy-a", "boy-b"),
                List.of("girl-a", "girl-b"),
                List.of("neutral-child"),
                List.of("teen-m-a", "teen-m-b"),
                List.of("teen-f-a", "teen-f-b"),
                List.of("neutral-teen"),
                List.of("man-a", "man-b", "man-c"),
                List.of("woman-a", "woman-b", "woman-c"),
                List.of("neutral-adult"),
                List.of("global-a", "global-b"),
                "legacy"
        ));
    }

    @Test
    void sameNpcAndBucketAlwaysResolveSameVoice() {
        UUID npc = UUID.fromString("11111111-2222-3333-4444-555555555555");
        NpcVoiceCatalog catalog = catalog();

        String first = catalog.select(npc, NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.ADULT);
        String second = catalog.select(npc, NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.ADULT);

        assertEquals(first, second);
        assertTrue(List.of("woman-a", "woman-b", "woman-c").contains(first));
    }

    @Test
    void exactAgeAndGenderPoolWins() {
        NpcVoiceCatalog catalog = catalog();
        UUID npc = new UUID(1L, 2L);

        assertTrue(List.of("boy-a", "boy-b").contains(
                catalog.select(npc, NpcVoiceGender.MALE, NpcVoiceAgeGroup.CHILD)));
        assertTrue(List.of("teen-f-a", "teen-f-b").contains(
                catalog.select(npc, NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.TEEN)));
    }

    @Test
    void missingYoungPoolFallsBackToSameGenderAdultBeforeNeutral() {
        NpcVoiceCatalog sparse = new NpcVoiceCatalog(new NpcVoiceCatalog.VoicePools(
                List.of(), List.of(), List.of("neutral-child"),
                List.of(), List.of(), List.of("neutral-teen"),
                List.of("man"), List.of("woman"), List.of("neutral-adult"),
                List.of("global"), "legacy"
        ));

        assertEquals("woman", sparse.select(new UUID(3L, 4L), NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.CHILD));
        assertEquals("man", sparse.select(new UUID(5L, 6L), NpcVoiceGender.MALE, NpcVoiceAgeGroup.TEEN));
    }

    @Test
    void neutralYoungNpcFallsBackToNeutralAdultBeforeGlobal() {
        NpcVoiceCatalog sparse = new NpcVoiceCatalog(new NpcVoiceCatalog.VoicePools(
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of("man"), List.of("woman"), List.of("neutral-adult"),
                List.of("global"), "legacy"
        ));

        assertEquals("neutral-adult", sparse.select(new UUID(13L, 14L), NpcVoiceGender.NEUTRAL, NpcVoiceAgeGroup.CHILD));
    }

    @Test
    void neutralThenGlobalThenLegacyAreSafeFallbacks() {
        NpcVoiceCatalog neutral = new NpcVoiceCatalog(new NpcVoiceCatalog.VoicePools(
                List.of(), List.of(), List.of("neutral-child"),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of("global"), "legacy"
        ));
        assertEquals("neutral-child", neutral.select(new UUID(7L, 8L), NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.CHILD));

        NpcVoiceCatalog global = new NpcVoiceCatalog(new NpcVoiceCatalog.VoicePools(
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of("global"), "legacy"
        ));
        assertEquals("global", global.select(new UUID(9L, 10L), NpcVoiceGender.NEUTRAL, NpcVoiceAgeGroup.TEEN));

        NpcVoiceCatalog legacy = new NpcVoiceCatalog(new NpcVoiceCatalog.VoicePools(
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(), "legacy"
        ));
        assertEquals("legacy", legacy.select(new UUID(11L, 12L), NpcVoiceGender.MALE, NpcVoiceAgeGroup.ADULT));
    }

    @Test
    void reportsWhetherStoredVoiceIsStillEligibleForBucket() {
        NpcVoiceCatalog catalog = catalog();

        assertTrue(catalog.isEligible("girl-a", NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.CHILD));
        assertTrue(catalog.isEligible("woman-b", NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.CHILD));
        assertFalse(catalog.isEligible("man-a", NpcVoiceGender.FEMALE, NpcVoiceAgeGroup.CHILD));
    }
}
