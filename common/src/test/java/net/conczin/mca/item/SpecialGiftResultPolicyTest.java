package net.conczin.mca.item;

import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialGiftResultPolicyTest {
    @Test
    void passFallsThroughWithoutConsumption() {
        assertFalse(SpecialGiftResultPolicy.isHandled(InteractionResult.PASS));
        assertFalse(SpecialGiftResultPolicy.shouldConsume(InteractionResult.PASS));
    }

    @Test
    void failureIsHandledWithoutConsumption() {
        assertTrue(SpecialGiftResultPolicy.isHandled(InteractionResult.FAIL));
        assertFalse(SpecialGiftResultPolicy.shouldConsume(InteractionResult.FAIL));
    }

    @Test
    void consumeIsHandledAndConsumesExactlyOne() {
        assertTrue(SpecialGiftResultPolicy.isHandled(InteractionResult.CONSUME));
        assertTrue(SpecialGiftResultPolicy.shouldConsume(InteractionResult.CONSUME));
    }

    @Test
    void successIsHandledButDoesNotImplyItemConsumption() {
        assertTrue(SpecialGiftResultPolicy.isHandled(InteractionResult.SUCCESS));
        assertFalse(SpecialGiftResultPolicy.shouldConsume(InteractionResult.SUCCESS));
    }
}
