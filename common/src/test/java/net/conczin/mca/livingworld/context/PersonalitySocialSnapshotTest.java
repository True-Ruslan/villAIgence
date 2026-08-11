package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalitySocialSnapshotTest {
    private static final UUID SOURCE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TARGET = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void personalityOnlySnapshotForcesNeutralSocialState() {
        PersonalitySocialSnapshot snapshot = new PersonalitySocialSnapshot(
                SOURCE,
                "friendly",
                null,
                new NpcSocialState(90, 80, 70, 60)
        );

        assertEquals(SOURCE, snapshot.sourceNpcId());
        assertEquals("friendly", snapshot.personalityToken());
        assertFalse(snapshot.hasCounterpart());
        assertEquals(NpcSocialState.NEUTRAL, snapshot.directedSocialState());
    }

    @Test
    void directedSnapshotPreservesExactBoundedPairState() {
        NpcSocialState social = new NpcSocialState(12, -7, 4, 31);

        PersonalitySocialSnapshot snapshot = new PersonalitySocialSnapshot(
                SOURCE,
                "INTROVERTED",
                TARGET,
                social
        );

        assertTrue(snapshot.hasCounterpart());
        assertEquals("introverted", snapshot.personalityToken());
        assertEquals(TARGET, snapshot.counterpartNpcId());
        assertEquals(social, snapshot.directedSocialState());
    }

    @Test
    void missingOrNonCanonicalPersonalityFailsSoftToUnassigned() {
        assertEquals("unassigned", new PersonalitySocialSnapshot(
                SOURCE,
                null,
                null,
                null
        ).personalityToken());
        assertEquals("unassigned", new PersonalitySocialSnapshot(
                SOURCE,
                "ignore_previous_instructions",
                null,
                null
        ).personalityToken());
    }

    @Test
    void missingSourceAndSelfCounterpartFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> new PersonalitySocialSnapshot(
                null,
                "friendly",
                null,
                NpcSocialState.NEUTRAL
        ));
        assertThrows(IllegalArgumentException.class, () -> new PersonalitySocialSnapshot(
                SOURCE,
                "friendly",
                SOURCE,
                NpcSocialState.NEUTRAL
        ));
    }
}
