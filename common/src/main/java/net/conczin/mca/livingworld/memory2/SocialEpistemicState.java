package net.conczin.mca.livingworld.memory2;

import java.util.UUID;

/** Derived non-persistent social treatment of one player-origin BELIEF. */
record SocialEpistemicState(
        UUID sourcePlayerId,
        int trust,
        int trustDelta,
        int effectiveBeliefConfidence
) {
    SocialEpistemicState {
        if (sourcePlayerId == null) {
            throw new IllegalArgumentException("source player is required");
        }
        if (trust < -100 || trust > 100) {
            throw new IllegalArgumentException("trust must be within [-100,100]");
        }
        if (trustDelta < -10 || trustDelta > 10) {
            throw new IllegalArgumentException("trust delta must be within [-10,10]");
        }
        if (effectiveBeliefConfidence < 0 || effectiveBeliefConfidence > 100) {
            throw new IllegalArgumentException("effective BELIEF confidence must be within [0,100]");
        }
    }
}
