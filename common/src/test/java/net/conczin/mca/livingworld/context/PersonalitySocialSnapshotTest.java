package net.conczin.mca.livingworld.context;

import net.conczin.mca.entity.ai.relationship.Personality;
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
                Personality.FRIENDLY,
                null,
                new NpcSocialState(90, 80, 70, 60)
        );

        assertEquals(SOURCE, snapshot.sourceNpcId());
        assertEquals(Personality.FRIENDLY, snapshot.personality());
        assertFalse(snapshot.hasCounterpart());
        assertEquals(NpcSocialState.NEUTRAL, snapshot.directedSocialState());
    }

    @Test
    void directedSnapshotPreservesExactBoundedPairState() {
        NpcSocialState social = new NpcSocialState(12, -7, 4, 31);

        PersonalitySocialSnapshot snapshot = new PersonalitySocialSnapshot(
                SOURCE,
                Personality.INTROVERTED,
                TARGET,
                social
        );

        assertTrue(snapshot.hasCounterpart());
        assertEquals(TARGET, snapshot.counterpartNpcId());
        assertEquals(social, snapshot.directedSocialState());
    }

    @Test
    void nullPersonalityFailsSoftToUnassigned() {
        PersonalitySocialSnapshot snapshot = new PersonalitySocialSnapshot(
                SOURCE,
                null,
                null,
                null
        );

        assertEquals(Personality.UNASSIGNED, snapshot.personality());
        assertEquals(NpcSocialState.NEUTRAL, snapshot.directedSocialState());
    }

    @Test
    void missingSourceAndSelfCounterpartFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> new PersonalitySocialSnapshot(
                null,
                Personality.FRIENDLY,
                null,
                NpcSocialState.NEUTRAL
        ));
        assertThrows(IllegalArgumentException.class, () -> new PersonalitySocialSnapshot(
                SOURCE,
                Personality.FRIENDLY,
                SOURCE,
                NpcSocialState.NEUTRAL
        ));
    }
}
