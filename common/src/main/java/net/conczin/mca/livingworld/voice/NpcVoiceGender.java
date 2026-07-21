package net.conczin.mca.livingworld.voice;

/** Provider-neutral gender bucket used only for voice selection. */
public enum NpcVoiceGender {
    MALE,
    FEMALE,
    NEUTRAL;

    public static NpcVoiceGender fromName(String value) {
        if (value == null) return NEUTRAL;
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "male" -> MALE;
            case "female" -> FEMALE;
            default -> NEUTRAL;
        };
    }
}
