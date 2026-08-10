package net.conczin.mca.livingworld.relationship;

/** Server-validated directed NPC-to-NPC social-state delta. */
public record NpcSocialDelta(int trust, int respect, int fear, int affinity) {
    public static final NpcSocialDelta NONE = new NpcSocialDelta(0, 0, 0, 0);

    public NpcSocialDelta sanitized(int maxMagnitude) {
        long magnitude = Math.abs((long) maxMagnitude);
        int max = (int) Math.min(Integer.MAX_VALUE, magnitude);
        return new NpcSocialDelta(
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
