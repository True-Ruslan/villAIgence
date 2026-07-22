package net.conczin.mca.livingworld.memory2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Converts one successful player-to-NPC dialogue turn into a bounded belief MemoryEvent. */
public final class DialogueMemoryAdapter {
    static final int MAX_UTTERANCE_CODE_POINTS = 240;
    private static final String ID_NAMESPACE = "memory2-dialogue-v1";

    private DialogueMemoryAdapter() {
    }

    public static Optional<MemoryEvent> toMemoryEvent(
            UUID npcId,
            UUID playerId,
            long gameTime,
            String playerMessage,
            String npcReply,
            long createdAtEpochMillis
    ) {
        if (npcId == null || playerId == null) return Optional.empty();

        String normalizedPlayer = normalizeUtterance(playerMessage);
        String normalizedNpc = normalizeUtterance(npcReply);
        if (normalizedPlayer.isBlank() || normalizedNpc.isBlank()) return Optional.empty();

        long safeGameTime = Math.max(0L, gameTime);
        UUID id = deterministicId(npcId, playerId, safeGameTime, normalizedPlayer);
        String summary = "Player said: " + limitCodePoints(normalizedPlayer, MAX_UTTERANCE_CODE_POINTS)
                + " | NPC replied: " + limitCodePoints(normalizedNpc, MAX_UTTERANCE_CODE_POINTS);

        return Optional.of(new MemoryEvent(
                id,
                npcId,
                MemoryEvent.Type.DIALOGUE,
                summary,
                List.of(npcId, playerId),
                MemoryEvent.Provenance.PLAYER_TOLD,
                safeGameTime,
                createdAtEpochMillis,
                40,
                0,
                60,
                List.of()
        ));
    }

    private static UUID deterministicId(UUID npcId, UUID playerId, long gameTime, String normalizedPlayerMessage) {
        String canonical = ID_NAMESPACE
                + '\n' + npcId
                + '\n' + playerId
                + '\n' + gameTime
                + '\n' + normalizedPlayerMessage;
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizeUtterance(String value) {
        if (value == null || value.isBlank()) return "";
        StringBuilder output = new StringBuilder(value.length());
        boolean previousWhitespace = false;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            boolean whitespace = Character.isWhitespace(codePoint) || Character.isISOControl(codePoint);
            if (whitespace) {
                if (!previousWhitespace && output.length() > 0) output.append(' ');
                previousWhitespace = true;
            } else {
                output.appendCodePoint(codePoint);
                previousWhitespace = false;
            }
        }
        return output.toString().strip();
    }

    private static String limitCodePoints(String value, int maxCodePoints) {
        if (value.codePointCount(0, value.length()) <= maxCodePoints) return value;
        int end = value.offsetByCodePoints(0, maxCodePoints);
        return value.substring(0, end);
    }
}
