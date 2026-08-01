package net.conczin.mca.livingworld.lore.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorLoreProtocolContractTest {
    @Test
    void requestIdsAdvanceMonotonicallyAndWrapToOne() {
        assertEquals(1, OperatorLoreProtocolPolicy.nextRequestId(0));
        assertEquals(42, OperatorLoreProtocolPolicy.nextRequestId(41));
        assertEquals(1, OperatorLoreProtocolPolicy.nextRequestId(Integer.MAX_VALUE));
    }

    @Test
    void onlyTheCurrentPositiveRequestMatchesAResponse() {
        assertTrue(OperatorLoreProtocolPolicy.matches(41, 41));
        assertFalse(OperatorLoreProtocolPolicy.matches(41, 40));
        assertFalse(OperatorLoreProtocolPolicy.matches(0, 0));
        assertFalse(OperatorLoreProtocolPolicy.matches(-1, -1));
    }

    @Test
    void serverEchoDoesNotTransformTheCorrelationValue() {
        assertEquals(43, OperatorLoreProtocolPolicy.echo(43));
        assertEquals(Integer.MIN_VALUE, OperatorLoreProtocolPolicy.echo(Integer.MIN_VALUE));
    }
}
