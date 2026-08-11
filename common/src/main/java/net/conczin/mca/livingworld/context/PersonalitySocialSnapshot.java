package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.relationship.NpcSocialState;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Fixed-size immutable server-owned personality and optional direct NPC-pair social state. */
public record PersonalitySocialSnapshot(
        UUID sourceNpcId,
        String personalityToken,
        UUID counterpartNpcId,
        NpcSocialState directedSocialState
) {
    private static final Set<String> CANONICAL_PERSONALITY_TOKENS = Set.of(
            "unassigned",
            "friendly",
            "flirty",
            "playful",
            "gloomy",
            "sensitive",
            "greedy",
            "odd",
            "crabby",
            "extroverted",
            "introverted",
            "relaxed",
            "anxious",
            "peaceful",
            "upbeat"
    );

    public PersonalitySocialSnapshot {
        if (sourceNpcId == null) {
            throw new IllegalArgumentException("sourceNpcId is required");
        }
        if (sourceNpcId.equals(counterpartNpcId)) {
            throw new IllegalArgumentException("NPC social snapshot cannot target itself");
        }
        personalityToken = canonicalPersonalityToken(personalityToken);
        directedSocialState = counterpartNpcId == null
                ? NpcSocialState.NEUTRAL
                : directedSocialState == null ? NpcSocialState.NEUTRAL : directedSocialState;
    }

    public boolean hasCounterpart() {
        return counterpartNpcId != null;
    }

    public static String canonicalPersonalityToken(String rawToken) {
        if (rawToken == null) return "unassigned";
        String normalized = rawToken.trim().toLowerCase(Locale.ROOT);
        return CANONICAL_PERSONALITY_TOKENS.contains(normalized) ? normalized : "unassigned";
    }
}
