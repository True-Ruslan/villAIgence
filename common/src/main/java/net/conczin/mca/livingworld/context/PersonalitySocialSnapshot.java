package net.conczin.mca.livingworld.context;

import net.conczin.mca.entity.ai.relationship.Personality;
import net.conczin.mca.livingworld.relationship.NpcSocialState;

import java.util.Locale;
import java.util.UUID;

/** Fixed-size immutable server-owned personality and optional direct NPC-pair social state. */
public record PersonalitySocialSnapshot(
        UUID sourceNpcId,
        Personality personality,
        UUID counterpartNpcId,
        NpcSocialState directedSocialState
) {
    public PersonalitySocialSnapshot {
        if (sourceNpcId == null) {
            throw new IllegalArgumentException("sourceNpcId is required");
        }
        if (sourceNpcId.equals(counterpartNpcId)) {
            throw new IllegalArgumentException("NPC social snapshot cannot target itself");
        }
        personality = personality == null ? Personality.UNASSIGNED : personality;
        directedSocialState = counterpartNpcId == null
                ? NpcSocialState.NEUTRAL
                : directedSocialState == null ? NpcSocialState.NEUTRAL : directedSocialState;
    }

    /** Compatibility boundary for callers that still provide the persisted/canonical token form. */
    public PersonalitySocialSnapshot(
            UUID sourceNpcId,
            String personalityToken,
            UUID counterpartNpcId,
            NpcSocialState directedSocialState
    ) {
        this(sourceNpcId, personalityFromToken(personalityToken), counterpartNpcId, directedSocialState);
    }

    public boolean hasCounterpart() {
        return counterpartNpcId != null;
    }

    public String personalityToken() {
        return personality.name().toLowerCase(Locale.ROOT);
    }

    public static String canonicalPersonalityToken(String rawToken) {
        return personalityFromToken(rawToken).name().toLowerCase(Locale.ROOT);
    }

    private static Personality personalityFromToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Personality.UNASSIGNED;
        try {
            return Personality.valueOf(rawToken.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Personality.UNASSIGNED;
        }
    }
}
