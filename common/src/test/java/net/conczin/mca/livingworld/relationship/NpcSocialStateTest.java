package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcSocialStateTest {
    @Test
    void stateAlwaysClampsToNpcSocialBounds() {
        NpcSocialState state = new NpcSocialState(500, -500, 101, -101);

        assertEquals(100, state.trust());
        assertEquals(-100, state.respect());
        assertEquals(100, state.fear());
        assertEquals(-100, state.affinity());
    }

    @Test
    void neutralStateIsExplicitAndStable() {
        assertEquals(new NpcSocialState(0, 0, 0, 0), NpcSocialState.NEUTRAL);
        assertEquals(NpcSocialState.NEUTRAL, NpcSocialState.NEUTRAL.apply(null, 10));
    }

    @Test
    void proposedDeltaIsClampedBeforeApplying() {
        NpcSocialState state = new NpcSocialState(10, 10, 10, 10);
        NpcSocialDelta proposed = new NpcSocialDelta(1000, -1000, 3, -3);

        NpcSocialState updated = state.apply(proposed, 2);

        assertEquals(new NpcSocialState(12, 8, 12, 8), updated);
    }

    @Test
    void finalStateSaturatesAtGlobalBounds() {
        NpcSocialState state = new NpcSocialState(99, -99, 99, -99);
        NpcSocialDelta proposed = new NpcSocialDelta(2, -2, 2, -2);

        assertEquals(new NpcSocialState(100, -100, 100, -100), state.apply(proposed, 2));
    }

    @Test
    void negativeMutationLimitIsTreatedAsAbsoluteMagnitude() {
        NpcSocialDelta proposed = new NpcSocialDelta(8, -8, 1, -1);

        assertEquals(new NpcSocialDelta(3, -3, 1, -1), proposed.sanitized(-3));
    }
}