package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LivingWorldRelationshipStateTest {
    @Test
    void stateAlwaysClampsToWorldBounds() {
        LivingWorldRelationshipState state = new LivingWorldRelationshipState(500, -500, 101, -101);
        assertEquals(100, state.trust());
        assertEquals(-100, state.respect());
        assertEquals(100, state.fear());
        assertEquals(-100, state.affinity());
    }

    @Test
    void proposedDeltaIsClampedPerTurnBeforeApplying() {
        LivingWorldRelationshipState state = new LivingWorldRelationshipState(10, 10, 10, 10);
        LivingWorldRelationshipDelta proposed = new LivingWorldRelationshipDelta(1000, -1000, 3, -3);

        LivingWorldRelationshipState updated = state.apply(proposed, 2);

        assertEquals(new LivingWorldRelationshipState(12, 8, 12, 8), updated);
    }

    @Test
    void finalStateSaturatesAtRelationshipBounds() {
        LivingWorldRelationshipState state = new LivingWorldRelationshipState(99, -99, 99, -99);
        LivingWorldRelationshipDelta proposed = new LivingWorldRelationshipDelta(2, -2, 2, -2);

        assertEquals(new LivingWorldRelationshipState(100, -100, 100, -100), state.apply(proposed, 2));
    }
}
