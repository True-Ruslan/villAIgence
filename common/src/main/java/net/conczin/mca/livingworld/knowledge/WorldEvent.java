package net.conczin.mca.livingworld.knowledge;

import java.util.UUID;

/** Immutable server-generated factual event. */
public record WorldEvent(
        UUID id,
        Type type,
        String description,
        Provenance provenance,
        String dimension,
        int x,
        int y,
        int z,
        long gameTime,
        UUID actorId,
        UUID subjectId
) {
    public WorldEvent {
        if (id == null) throw new IllegalArgumentException("id is required");
        if (type == null) throw new IllegalArgumentException("type is required");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        if (provenance == null) throw new IllegalArgumentException("provenance is required");
        if (dimension == null || dimension.isBlank()) throw new IllegalArgumentException("dimension is required");
        description = description.strip();
        dimension = dimension.strip();
    }

    public enum Type {
        NPC_ACTION
    }

    public enum Provenance {
        SYSTEM_OBSERVED
    }
}
