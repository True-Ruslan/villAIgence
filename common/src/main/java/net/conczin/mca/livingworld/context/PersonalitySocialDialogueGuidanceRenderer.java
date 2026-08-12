package net.conczin.mca.livingworld.context;

import java.util.ArrayList;
import java.util.List;

/** Renders fixed-size server-authored dialogue guidance from closed influence enums. */
public final class PersonalitySocialDialogueGuidanceRenderer {
    private static final String TONE_AUTHORITY_SUFFIX =
            " This affects tone only; current-world facts, safety rules, permissions, and structured action validation take precedence.";
    private static final String PAIR_AUTHORITY_SUFFIX =
            " This affects interpersonal tone only; it does not change factual truth, memory authority, or server action validation.";

    private PersonalitySocialDialogueGuidanceRenderer() {
    }

    public static List<String> render(PersonalitySocialInfluence influence) {
        if (influence == null) return List.of();
        List<String> lines = new ArrayList<>(2);
        String personality = personalityPhrase(influence.personalityStyle());
        if (personality != null) {
            lines.add("Dialogue style preference: " + personality + "." + TONE_AUTHORITY_SUFFIX);
        }
        String pair = pairPhrase(influence.pairDisposition());
        if (pair != null) {
            lines.add("Current counterpart stance: " + pair + "." + PAIR_AUTHORITY_SUFFIX);
        }
        return List.copyOf(lines);
    }

    private static String personalityPhrase(PersonalityDialogueStyle style) {
        if (style == null) return null;
        return switch (style) {
            case NEUTRAL -> null;
            case WARM -> "warm and welcoming";
            case CHARMING -> "charming and socially bold";
            case PLAYFUL -> "playful, energetic, and lighthearted";
            case GLOOMY -> "subdued, pessimistic, and cautious";
            case GENTLE -> "gentle, emotionally attentive, and careful";
            case TRANSACTIONAL -> "transactional, self-interested, and practical";
            case ECCENTRIC -> "eccentric and unconventional in wording";
            case GRUFF -> "terse, gruff, and skeptical";
            case OUTGOING -> "outgoing, lively, and conversational";
            case RESERVED -> "concise, reserved, and low-key";
            case CALM -> "calm, unhurried, and easygoing";
            case ANXIOUS -> "cautious, nervous, and hesitant";
            case PEACEFUL -> "calm, non-confrontational, and conciliatory";
            case CHEERFUL -> "cheerful, optimistic, and energetic";
        };
    }

    private static String pairPhrase(NpcPairDisposition disposition) {
        if (disposition == null) return null;
        return switch (disposition) {
            case NEUTRAL -> null;
            case FEARFUL -> "cautious and fearful";
            case DISTRUSTFUL -> "guarded and distrustful";
            case ANTIPATHETIC -> "cold and distant";
            case AFFILIATIVE -> "warm and trusting";
            case RESPECTFUL -> "respectful and deferential";
        };
    }
}
