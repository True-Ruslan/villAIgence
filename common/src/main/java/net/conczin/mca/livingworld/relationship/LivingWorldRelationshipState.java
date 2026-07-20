package net.conczin.mca.livingworld.relationship;

/** Persistent server-owned social state independent from legacy MCA hearts. */
public record LivingWorldRelationshipState(int trust, int respect, int fear, int affinity) {
    public static final int MIN_VALUE = -100;
    public static final int MAX_VALUE = 100;
    public static final LivingWorldRelationshipState NEUTRAL = new LivingWorldRelationshipState(0, 0, 0, 0);

    public LivingWorldRelationshipState {
        trust = clamp(trust);
        respect = clamp(respect);
        fear = clamp(fear);
        affinity = clamp(affinity);
    }

    public LivingWorldRelationshipState apply(LivingWorldRelationshipDelta proposed, int maxDeltaPerTurn) {
        if (proposed == null) return this;
        LivingWorldRelationshipDelta delta = proposed.sanitized(maxDeltaPerTurn);
        return new LivingWorldRelationshipState(
                addBounded(trust, delta.trust()),
                addBounded(respect, delta.respect()),
                addBounded(fear, delta.fear()),
                addBounded(affinity, delta.affinity())
        );
    }

    public String factualSummary() {
        return "LivingWorld social state with player: trust=" + trust
                + ", respect=" + respect
                + ", fear=" + fear
                + ", affinity=" + affinity + ".";
    }

    private static int addBounded(int current, int delta) {
        long result = (long) current + delta;
        if (result > MAX_VALUE) return MAX_VALUE;
        if (result < MIN_VALUE) return MIN_VALUE;
        return (int) result;
    }

    private static int clamp(int value) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
    }
}
