package net.conczin.mca.livingworld.relationship;

/** Exact server-applied relationship transition after validation, clamping and persistence. */
public record LivingWorldRelationshipChange(
        LivingWorldRelationshipState before,
        LivingWorldRelationshipState after,
        LivingWorldRelationshipDelta appliedDelta
) {
    public LivingWorldRelationshipChange {
        before = before == null ? LivingWorldRelationshipState.NEUTRAL : before;
        after = after == null ? LivingWorldRelationshipState.NEUTRAL : after;
        appliedDelta = difference(before, after);
    }

    public static LivingWorldRelationshipChange between(
            LivingWorldRelationshipState before,
            LivingWorldRelationshipState after
    ) {
        return new LivingWorldRelationshipChange(before, after, LivingWorldRelationshipDelta.NONE);
    }

    public boolean changed() {
        return !before.equals(after);
    }

    private static LivingWorldRelationshipDelta difference(
            LivingWorldRelationshipState before,
            LivingWorldRelationshipState after
    ) {
        return new LivingWorldRelationshipDelta(
                after.trust() - before.trust(),
                after.respect() - before.respect(),
                after.fear() - before.fear(),
                after.affinity() - before.affinity()
        );
    }
}
