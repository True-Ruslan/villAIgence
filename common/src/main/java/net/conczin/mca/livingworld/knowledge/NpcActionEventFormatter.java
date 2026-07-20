package net.conczin.mca.livingworld.knowledge;

import java.util.Optional;

/** Converts only known safe MCA AI actions into deterministic factual descriptions. */
public final class NpcActionEventFormatter {
    private static final int MAX_NAME_CHARS = 48;

    private NpcActionEventFormatter() {
    }

    public static Optional<String> describe(String command, String villagerName, String playerName) {
        String villager = safeName(villagerName, "A villager");
        String player = safeName(playerName, "a player");
        return Optional.ofNullable(switch (command == null ? "" : command) {
            case "follow-player" -> villager + " started following " + player + ".";
            case "stay-here" -> villager + " agreed to stay at this location.";
            case "move-freely" -> villager + " resumed moving freely.";
            case "wear-armor" -> villager + " equipped available armor.";
            case "remove-armor" -> villager + " removed equipped armor.";
            case "try-go-home" -> villager + " tried to return home.";
            case "open-trade-window" -> villager + " opened trade with " + player + ".";
            default -> null;
        });
    }

    private static String safeName(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        StringBuilder normalized = new StringBuilder();
        boolean previousWhitespace = false;
        for (int offset = 0; offset < value.length() && normalized.codePointCount(0, normalized.length()) < MAX_NAME_CHARS; ) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) {
                if (!previousWhitespace && !normalized.isEmpty()) normalized.append(' ');
                previousWhitespace = true;
            } else if (!Character.isISOControl(codePoint)) {
                normalized.appendCodePoint(codePoint);
                previousWhitespace = false;
            }
        }
        String result = normalized.toString().strip();
        return result.isEmpty() ? fallback : result;
    }
}
