package net.conczin.mca.livingworld.memory2;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Explicit provenance-preserving input for controlled semantic BELIEF producers. */
public record SemanticBeliefSource(
        UUID ownerNpcId,
        String statement,
        List<UUID> relatedEntities,
        MemoryEvent.Provenance provenance,
        long gameTime,
        long createdAtEpochMillis,
        int importance,
        int confidence,
        List<UUID> sourceEventIds
) {
    public SemanticBeliefSource {
        if (ownerNpcId == null) throw new IllegalArgumentException("ownerNpcId is required");
        if (statement == null || statement.isBlank()) throw new IllegalArgumentException("statement is required");
        if (provenance == null || provenance == MemoryEvent.Provenance.SYSTEM_OBSERVED) {
            throw new IllegalArgumentException("BELIEF requires told or inferred provenance");
        }

        statement = statement.strip();
        relatedEntities = normalizeIds(relatedEntities);
        gameTime = Math.max(0L, gameTime);
        createdAtEpochMillis = Math.max(0L, createdAtEpochMillis);
        importance = clamp(importance, 0, 100);
        confidence = clamp(confidence, 0, 100);
        sourceEventIds = normalizeIds(sourceEventIds);
        if (sourceEventIds.isEmpty()) throw new IllegalArgumentException("BELIEF requires sourceEventIds");
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
}
