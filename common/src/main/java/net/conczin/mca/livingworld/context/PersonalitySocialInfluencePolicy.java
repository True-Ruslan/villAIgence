package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.relationship.NpcSocialState;

/** Pure deterministic interpretation of bounded server-owned personality/social state. */
public final class PersonalitySocialInfluencePolicy {
    static final int STRONG_FEAR = 75;
    static final int STRONG_NEGATIVE = -75;
    static final int STRONG_AFFINITY = 60;
    static final int STRONG_RESPECT = 70;

    private PersonalitySocialInfluencePolicy() {
    }

    public static PersonalitySocialInfluence evaluate(PersonalitySocialSnapshot snapshot) {
        if (snapshot == null) return PersonalitySocialInfluence.NEUTRAL;
        return new PersonalitySocialInfluence(
                personalityStyle(snapshot.personalityToken()),
                snapshot.hasCounterpart()
                        ? pairDisposition(snapshot.directedSocialState())
                        : NpcPairDisposition.NEUTRAL
        );
    }

    public static NpcPairDisposition pairDisposition(NpcSocialState state) {
        if (state == null) return NpcPairDisposition.NEUTRAL;
        if (state.fear() >= STRONG_FEAR) return NpcPairDisposition.FEARFUL;
        if (state.trust() <= STRONG_NEGATIVE) return NpcPairDisposition.DISTRUSTFUL;
        if (state.affinity() <= STRONG_NEGATIVE) return NpcPairDisposition.ANTIPATHETIC;
        if (state.trust() >= STRONG_AFFINITY && state.affinity() >= STRONG_AFFINITY) {
            return NpcPairDisposition.AFFILIATIVE;
        }
        if (state.respect() >= STRONG_RESPECT) return NpcPairDisposition.RESPECTFUL;
        return NpcPairDisposition.NEUTRAL;
    }

    private static PersonalityDialogueStyle personalityStyle(String token) {
        if (token == null) return PersonalityDialogueStyle.NEUTRAL;
        return switch (token) {
            case "friendly" -> PersonalityDialogueStyle.WARM;
            case "flirty" -> PersonalityDialogueStyle.CHARMING;
            case "playful" -> PersonalityDialogueStyle.PLAYFUL;
            case "gloomy" -> PersonalityDialogueStyle.GLOOMY;
            case "sensitive" -> PersonalityDialogueStyle.GENTLE;
            case "greedy" -> PersonalityDialogueStyle.TRANSACTIONAL;
            case "odd" -> PersonalityDialogueStyle.ECCENTRIC;
            case "crabby" -> PersonalityDialogueStyle.GRUFF;
            case "extroverted" -> PersonalityDialogueStyle.OUTGOING;
            case "introverted" -> PersonalityDialogueStyle.RESERVED;
            case "relaxed" -> PersonalityDialogueStyle.CALM;
            case "anxious" -> PersonalityDialogueStyle.ANXIOUS;
            case "peaceful" -> PersonalityDialogueStyle.PEACEFUL;
            case "upbeat" -> PersonalityDialogueStyle.CHEERFUL;
            default -> PersonalityDialogueStyle.NEUTRAL;
        };
    }
}
