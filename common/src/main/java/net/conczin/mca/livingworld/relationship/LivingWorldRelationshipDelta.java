package net.conczin.mca.livingworld.relationship;

/** LLM-proposed relationship change before server-side validation/clamping. */
public record LivingWorldRelationshipDelta(int trust, int respect, int fear, int affinity) {
    public static final LivingWorldRelationshipDelta NONE = new LivingWorldRelationshipDelta(0, 0, 0, 0);

    public LivingWorldRelationshipDelta sanitized(int maxMagnitude) {
        int max = Math.max(0, maxMagnitude);
        return new LivingWorldRelationshipDelta(
                clamp(trust, -max, max),
                clamp(respect, -max, max),
                clamp(fear, -max, max),
                clamp(affinity, -max, max)
        );
    }

    public boolean isZero() {
        return trust == 0 && respect == 0 && fear == 0 && affinity == 0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
