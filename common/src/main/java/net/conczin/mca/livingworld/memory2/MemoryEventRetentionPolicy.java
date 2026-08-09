package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure provider-independent retention pressure and game-time decay policy for episodic/social Memory 2.0. */
public final class MemoryEventRetentionPolicy {
    static final long DECAY_STEP_TICKS = 36_000L;

    private static final Comparator<MemoryEvent> PERSISTENCE_ORDER = Comparator
            .comparingLong(MemoryEvent::gameTime)
            .thenComparingLong(MemoryEvent::createdAtEpochMillis)
            .thenComparing(event -> event.id().toString());

    private MemoryEventRetentionPolicy() {
    }

    public static List<MemoryEvent> selectRetained(
            List<MemoryEvent> events,
            int maxEvents,
            long nowGameTime
    ) {
        if (events == null || events.isEmpty()) return List.of();

        int safeMax = Math.max(1, maxEvents);
        long safeNow = Math.max(0L, nowGameTime);
        Map<UUID, MemoryEvent> uniqueById = new LinkedHashMap<>();
        for (MemoryEvent event : events) {
            if (event != null) uniqueById.putIfAbsent(event.id(), event);
        }
        if (uniqueById.isEmpty()) return List.of();

        List<MemoryEvent> candidates = new ArrayList<>(uniqueById.values());
        if (candidates.size() > safeMax) {
            candidates.sort(retentionOrder(safeNow));
            candidates = new ArrayList<>(candidates.subList(0, safeMax));
        }

        candidates.sort(PERSISTENCE_ORDER);
        return List.copyOf(candidates);
    }

    static int durabilityScore(MemoryEvent event) {
        if (event == null) return 0;
        return event.importance() * 4
                + event.confidence() * 5 / 2
                + Math.abs(event.emotionalWeight()) * 2
                + provenanceContribution(event.provenance())
                + typeContribution(event.type());
    }

    static long effectiveRetentionScore(MemoryEvent event, long nowGameTime) {
        if (event == null) return Long.MIN_VALUE;
        long safeNow = Math.max(0L, nowGameTime);
        long ageTicks = Math.max(0L, safeNow - event.gameTime());
        return (long) durabilityScore(event) * DECAY_STEP_TICKS - ageTicks;
    }

    static int typeContribution(MemoryEvent.Type type) {
        if (type == null) return 0;
        return switch (type) {
            case RELATIONSHIP_CAUSE -> 300;
            case RELATIONSHIP_CHANGE -> 225;
            case OBSERVATION, ACTION, SEMANTIC_CONTRADICTION -> 125;
            case DIALOGUE -> 0;
        };
    }

    static int provenanceContribution(MemoryEvent.Provenance provenance) {
        if (provenance == null) return 0;
        return switch (provenance) {
            case SYSTEM_OBSERVED -> 200;
            case PLAYER_TOLD -> 100;
            case NPC_TOLD -> 75;
            case INFERRED -> 25;
        };
    }

    private static Comparator<MemoryEvent> retentionOrder(long nowGameTime) {
        return Comparator
                .comparingLong((MemoryEvent event) -> effectiveRetentionScore(event, nowGameTime))
                .reversed()
                .thenComparing(Comparator.comparingInt(MemoryEvent::importance).reversed())
                .thenComparing(Comparator.comparingInt(MemoryEvent::confidence).reversed())
                .thenComparing(Comparator.comparingInt(
                        (MemoryEvent event) -> Math.abs(event.emotionalWeight())).reversed())
                .thenComparing(Comparator.comparingInt(
                        (MemoryEvent event) -> typeContribution(event.type())).reversed())
                .thenComparing(Comparator.comparingInt(
                        (MemoryEvent event) -> provenanceContribution(event.provenance())).reversed())
                .thenComparing(Comparator.comparingLong(MemoryEvent::gameTime).reversed())
                .thenComparing(Comparator.comparingLong(MemoryEvent::createdAtEpochMillis).reversed())
                .thenComparing(event -> event.id().toString());
    }
}
