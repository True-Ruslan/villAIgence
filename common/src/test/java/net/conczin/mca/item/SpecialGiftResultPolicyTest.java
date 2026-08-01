package net.conczin.mca.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialGiftResultPolicyTest {
    @Test
    void passFallsThroughWithoutConsumption() {
        SpecialGiftResultPolicy.Decision decision = SpecialGiftResultPolicy.decide(true, false);

        assertFalse(decision.handled());
        assertFalse(decision.consume());
    }

    @Test
    void failureIsHandledWithoutConsumption() {
        SpecialGiftResultPolicy.Decision decision = SpecialGiftResultPolicy.decide(false, false);

        assertTrue(decision.handled());
        assertFalse(decision.consume());
    }

    @Test
    void consumeIsHandledAndConsumesExactlyOne() {
        SpecialGiftResultPolicy.Decision decision = SpecialGiftResultPolicy.decide(false, true);

        assertTrue(decision.handled());
        assertTrue(decision.consume());
    }

    @Test
    void passNeverConsumesEvenForInvalidInputCombination() {
        SpecialGiftResultPolicy.Decision decision = SpecialGiftResultPolicy.decide(true, true);

        assertFalse(decision.handled());
        assertFalse(decision.consume());
    }
}
