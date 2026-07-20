package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingWorldRelationshipActionPolicyTest {
    @Test
    void followRequiresMinimumTrustAndAcceptableFear() {
        assertTrue(LivingWorldRelationshipActionPolicy.isAllowed(
                "follow-player", new LivingWorldRelationshipState(-25, 0, 60, 0)));
        assertFalse(LivingWorldRelationshipActionPolicy.isAllowed(
                "follow-player", new LivingWorldRelationshipState(-26, 0, 0, 0)));
        assertFalse(LivingWorldRelationshipActionPolicy.isAllowed(
                "follow-player", new LivingWorldRelationshipState(100, 0, 61, 0)));
    }

    @Test
    void otherCurrentSafeActionsRemainUnchanged() {
        LivingWorldRelationshipState hostile = new LivingWorldRelationshipState(-100, -100, 100, -100);
        assertTrue(LivingWorldRelationshipActionPolicy.isAllowed("stay-here", hostile));
        assertTrue(LivingWorldRelationshipActionPolicy.isAllowed("open-trade-window", hostile));
        assertTrue(LivingWorldRelationshipActionPolicy.isAllowed("try-go-home", hostile));
    }
}
