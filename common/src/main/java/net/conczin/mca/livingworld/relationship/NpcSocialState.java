package net.conczin.mca.livingworld.relationship;

/** Persistent server-owned directed social state from one NPC to another NPC. */
public record NpcSocialState(int trust, int respect, int fear, int affinity) {
    public static final int MIN_VALUE = -100;
    public static final int MAX_VALUE = 100;
    public static final NpcSocialState NEUTRAL = new NpcSocialState(0, 0, 0, 0);

    public NpcSocialState {
        trust = clamp(trust);
        respect = clamp(respect);
        fear = clamp(fear);
        affinity = clamp(affinity);
    }

    public NpcSocialState apply(NpcSocialDelta proposed, int maxDeltaPerMutation) {
        if (proposed == null) return this;
        NpcSocialDelta delta = proposed.sanitized(maxDeltaPerMutation);
        return new NpcSocialState(
                addBounded(trust, delta.trust()),
                addBounded(respect, delta.respect()),
                addBounded(fear, delta.fear()),
                addBounded(affinity, delta.affinity())
        );
    }

    public boolean isNeutral() {
        return equals(NEUTRAL);
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
