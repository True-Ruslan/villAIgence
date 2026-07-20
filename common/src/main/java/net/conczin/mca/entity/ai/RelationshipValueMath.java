package net.conczin.mca.entity.ai;

/** Numeric safety helpers for persisted relationship values. */
public final class RelationshipValueMath {
    private RelationshipValueMath() {
    }

    public static int saturatingAdd(int current, int delta) {
        long result = (long) current + delta;
        if (result > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (result < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) result;
    }
}
