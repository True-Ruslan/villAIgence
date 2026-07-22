package net.conczin.mca.livingworld.voice;

/** Resolves delivery mood from authoritative state and maps it to provider-neutral TTS hints. */
public final class NpcVoiceMoodResolver {
    private NpcVoiceMoodResolver() {
    }

    public static NpcVoiceMood resolve(
            boolean panicking,
            double healthRatio,
            int trust,
            int fear,
            int affinity
    ) {
        double safeHealth = Double.isFinite(healthRatio) ? Math.max(0.0D, Math.min(1.0D, healthRatio)) : 1.0D;
        if (panicking || fear >= 50) return NpcVoiceMood.AFRAID;
        if (safeHealth <= 0.30D) return NpcVoiceMood.SAD;
        if (trust <= -40) return NpcVoiceMood.ANGRY;
        if (trust >= 30 && affinity >= 40) return NpcVoiceMood.HAPPY;
        if (safeHealth <= 0.65D) return NpcVoiceMood.TIRED;
        return NpcVoiceMood.NEUTRAL;
    }

    public static TtsVoiceStyle style(NpcVoiceMood mood, NpcVoiceAgeGroup ageGroup) {
        NpcVoiceMood safeMood = mood == null ? NpcVoiceMood.NEUTRAL : mood;
        NpcVoiceAgeGroup safeAge = ageGroup == null ? NpcVoiceAgeGroup.ADULT : ageGroup;

        String ageInstruction = switch (safeAge) {
            case CHILD -> "Use a youthful, age-appropriate childlike delivery without caricature or baby talk.";
            case TEEN -> "Sound like a natural teenager: youthful but clear, without caricature.";
            case ADULT -> "Use a natural adult speaking voice.";
        };
        String moodInstruction = switch (safeMood) {
            case HAPPY -> "Sound warm, upbeat, and genuinely pleased.";
            case SAD -> "Sound subdued, quiet, and sad, with slower pacing.";
            case ANGRY -> "Use restrained irritation and firmness; do not shout or exaggerate.";
            case AFRAID -> "Sound tense and uneasy, with slightly quicker pacing; do not scream.";
            case TIRED -> "Sound tired and low-energy, with slightly slower pacing.";
            case NEUTRAL -> "Use a natural, conversational, emotionally neutral delivery.";
        };
        double speed = switch (safeMood) {
            case HAPPY -> 1.05D;
            case SAD -> 0.90D;
            case ANGRY -> 1.03D;
            case AFRAID -> 1.08D;
            case TIRED -> 0.92D;
            case NEUTRAL -> 1.0D;
        };
        return new TtsVoiceStyle(
                ageInstruction + " " + moodInstruction + " Keep the established character voice identity unchanged.",
                speed
        );
    }
}
