package net.conczin.mca.livingworld.relationship;

/** Conservative gameplay consequences derived from server-owned LivingWorld relationship state. */
public final class LivingWorldRelationshipActionPolicy {
    private static final int FOLLOW_MIN_TRUST = -25;
    private static final int FOLLOW_MAX_FEAR = 60;

    private LivingWorldRelationshipActionPolicy() {
    }

    public static boolean isAllowed(String commandName, LivingWorldRelationshipState state) {
        LivingWorldRelationshipState safe = state == null ? LivingWorldRelationshipState.NEUTRAL : state;
        if ("follow-player".equals(commandName)) {
            return safe.trust() >= FOLLOW_MIN_TRUST && safe.fear() <= FOLLOW_MAX_FEAR;
        }
        return true;
    }
}
