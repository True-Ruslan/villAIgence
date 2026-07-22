package net.conczin.mca.livingworld.memory2;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Immutable provider-independent semantic knowledge owned by one NPC. */
public record SemanticMemoryEntry(
        UUID id,
        UUID ownerNpcId,
        Kind kind,
        String statement,
        List<UUID> relatedEntities,
        MemoryEvent.Provenance provenance,
        long gameTime,
        long createdAtEpochMillis,
        int importance,
        int confidence,
        List<UUID> sourceEventIds
) {
    public SemanticMemoryEntry {
        if (id == null) throw new IllegalArgumentException("id is required");
        if (ownerNpcId == null) throw new IllegalArgumentException("ownerNpcId is required");
        if (kind == null) throw new IllegalArgumentException("kind is required");
        if (statement == null || statement.isBlank()) throw new IllegalArgumentException("statement is required");
        if (provenance == null) throw new IllegalArgumentException("provenance is required");
        validateTruthBoundary(kind, provenance);

        statement = statement.strip();
        relatedEntities = normalizeIds(relatedEntities);
        gameTime = Math.max(0L, gameTime);
        createdAtEpochMillis = Math.max(0L, createdAtEpochMillis);
        importance = clamp(importance, 0, 100);
        confidence = clamp(confidence, 0, 100);
        sourceEventIds = normalizeIds(sourceEventIds);
    }

    private static void validateTruthBoundary(Kind kind, MemoryEvent.Provenance provenance) {
        if (kind == Kind.FACT && provenance != MemoryEvent.Provenance.SYSTEM_OBSERVED) {
            throw new IllegalArgumentException("FACT requires SYSTEM_OBSERVED provenance");
        }
        if (kind == Kind.BELIEF && provenance == MemoryEvent.Provenance.SYSTEM_OBSERVED) {
            throw new IllegalArgumentException("SYSTEM_OBSERVED provenance must be represented as FACT");
        }
    }

    private static List<UUID> normalizeIds(List<UUID> values) {
        if (values == null || values.isEmpty()) return List.of();
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID value : values) {
            if (value != null) unique.add(value);
        }
        return List.copyOf(unique);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum Kind {
        FACT,
        BELIEF
    }
}
