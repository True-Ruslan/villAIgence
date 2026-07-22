package net.conczin.mca.livingworld.memory2;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Immutable bounded query for deterministic Memory 2.0 retrieval. */
public record MemoryQuery(
        UUID npcId,
        Set<UUID> participants,
        Set<MemoryEvent.Type> preferredTypes,
        long nowGameTime,
        long recencyHorizonTicks,
        int candidateLimit,
        int maxResults
) {
    private static final int MAX_CANDIDATES = 512;

    public MemoryQuery {
        if (npcId == null) throw new IllegalArgumentException("npcId is required");
        participants = normalizeParticipants(participants);
        preferredTypes = normalizeTypes(preferredTypes);
        nowGameTime = Math.max(0L, nowGameTime);
        recencyHorizonTicks = Math.max(1L, recencyHorizonTicks);
        candidateLimit = clamp(candidateLimit, 1, MAX_CANDIDATES);
        maxResults = clamp(maxResults, 1, candidateLimit);
    }

    private static Set<UUID> normalizeParticipants(Set<UUID> values) {
        if (values == null || values.isEmpty()) return Set.of();
        LinkedHashSet<UUID> normalized = new LinkedHashSet<>();
        for (UUID value : values) {
            if (value != null) normalized.add(value);
        }
        return Set.copyOf(normalized);
    }

    private static Set<MemoryEvent.Type> normalizeTypes(Set<MemoryEvent.Type> values) {
        if (values == null || values.isEmpty()) return Set.of();
        LinkedHashSet<MemoryEvent.Type> normalized = new LinkedHashSet<>();
        for (MemoryEvent.Type value : values) {
            if (value != null) normalized.add(value);
        }
        return Set.copyOf(normalized);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
