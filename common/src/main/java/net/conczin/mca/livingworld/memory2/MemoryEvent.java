package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Immutable provider-independent Memory 2.0 event owned by one NPC. */
public record MemoryEvent(
        UUID id,
        UUID ownerNpcId,
        Type type,
        String summary,
        List<UUID> participants,
        Provenance provenance,
        long gameTime,
        long createdAtEpochMillis,
        int importance,
        int emotionalWeight,
        int confidence,
        List<String> relationshipReasons
) {
    public MemoryEvent {
        if (id == null) throw new IllegalArgumentException("id is required");
        if (ownerNpcId == null) throw new IllegalArgumentException("ownerNpcId is required");
        if (type == null) throw new IllegalArgumentException("type is required");
        if (summary == null || summary.isBlank()) throw new IllegalArgumentException("summary is required");
        if (provenance == null) throw new IllegalArgumentException("provenance is required");

        summary = summary.strip();
        participants = normalizeParticipants(participants);
        gameTime = Math.max(0L, gameTime);
        createdAtEpochMillis = Math.max(0L, createdAtEpochMillis);
        importance = clamp(importance, 0, 100);
        emotionalWeight = clamp(emotionalWeight, -100, 100);
        confidence = clamp(confidence, 0, 100);
        relationshipReasons = normalizeReasons(relationshipReasons);
    }

    private static List<UUID> normalizeParticipants(List<UUID> participants) {
        if (participants == null || participants.isEmpty()) return List.of();
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID participant : participants) {
            if (participant != null) unique.add(participant);
        }
        return List.copyOf(unique);
    }

    private static List<String> normalizeReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) return List.of();
        Set<String> unique = new LinkedHashSet<>();
        for (String reason : reasons) {
            if (reason == null) continue;
            String normalized = reason.strip();
            if (!normalized.isBlank()) unique.add(normalized);
        }
        return List.copyOf(new ArrayList<>(unique));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum Type {
        DIALOGUE,
        OBSERVATION,
        ACTION,
        RELATIONSHIP_CHANGE
    }

    public enum Provenance {
        SYSTEM_OBSERVED,
        PLAYER_TOLD,
        NPC_TOLD,
        INFERRED
    }
}
