package net.conczin.mca.livingworld.voice;

/** Provider-neutral age bucket used for stable voice assignment. */
public enum NpcVoiceAgeGroup {
    CHILD,
    TEEN,
    ADULT;

    public static NpcVoiceAgeGroup fromName(String value) {
        if (value == null) return ADULT;
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "baby", "toddler", "child" -> CHILD;
            case "teen" -> TEEN;
            default -> ADULT;
        };
    }
}
