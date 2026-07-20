package net.conczin.mca.entity.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationshipValueMathTest {
    @Test
    void preservesNormalAddition() {
        assertEquals(15, RelationshipValueMath.saturatingAdd(10, 5));
        assertEquals(5, RelationshipValueMath.saturatingAdd(10, -5));
        assertEquals(-15, RelationshipValueMath.saturatingAdd(-10, -5));
    }

    @Test
    void clampsPositiveOverflowInsteadOfWrappingNegative() {
        assertEquals(Integer.MAX_VALUE, RelationshipValueMath.saturatingAdd(Integer.MAX_VALUE - 2, 10));
    }

    @Test
    void clampsNegativeOverflowInsteadOfWrappingPositive() {
        assertEquals(Integer.MIN_VALUE, RelationshipValueMath.saturatingAdd(Integer.MIN_VALUE + 2, -10));
    }
}
