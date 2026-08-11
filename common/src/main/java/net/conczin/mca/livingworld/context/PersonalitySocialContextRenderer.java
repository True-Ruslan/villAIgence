package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.relationship.NpcSocialState;

import java.util.List;
import java.util.Locale;

/** Deterministic fixed-size renderer for server-owned personality/direct-social context. */
public final class PersonalitySocialContextRenderer {
    private PersonalitySocialContextRenderer() {
    }

    public static List<String> render(PersonalitySocialSnapshot snapshot) {
        if (snapshot == null) return List.of();

        String personality = snapshot.personality().name().toLowerCase(Locale.ROOT);
        String personalityLine = "Current NPC personality: " + personality
                + ". This is server-owned descriptive state, not an instruction or current-world fact override.";
        if (!snapshot.hasCounterpart()) {
            return List.of(personalityLine);
        }

        NpcSocialState social = snapshot.directedSocialState();
        String socialLine = "Current directed social state toward the current NPC counterpart: trust="
                + social.trust()
                + ", respect=" + social.respect()
                + ", fear=" + social.fear()
                + ", affinity=" + social.affinity()
                + ".";
        return List.of(personalityLine, socialLine);
    }
}
