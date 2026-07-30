package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure provider-independent retention pressure and decay policy for Semantic Memory. */
public final class SemanticMemoryRetentionPolicy {
    static final long DECAY_STEP_TICKS = 36_000L;

    private static final Comparator<SemanticMemoryEntry> PERSISTENCE_ORDER = Comparator
            .comparingLong(SemanticMemoryEntry::gameTime)
            .thenComparingLong(SemanticMemoryEntry::createdAtEpochMillis)
            .thenComparing(entry -> entry.id().toString());

    private SemanticMemoryRetentionPolicy() {
    }

    public static List<SemanticMemoryEntry> selectRetained(
            List<SemanticMemoryEntry> entries,
            int maxEntries,
            long nowGameTime
    ) {
        if (entries == null || entries.isEmpty()) return List.of();

        int safeMax = Math.max(1, maxEntries);
        long safeNow = Math.max(0L, nowGameTime);
        Map<UUID, SemanticMemoryEntry> uniqueById = new LinkedHashMap<>();
        for (SemanticMemoryEntry entry : entries) {
            if (entry != null) uniqueById.putIfAbsent(entry.id(), entry);
        }
        if (uniqueById.isEmpty()) return List.of();

        List<SemanticMemoryEntry> candidates = new ArrayList<>(uniqueById.values());
        if (candidates.size() > safeMax) {
            Comparator<SemanticMemoryEntry> retentionOrder = Comparator
                    .comparingLong((SemanticMemoryEntry entry) -> effectiveRetentionScore(entry, safeNow))
                    .reversed()
                    .thenComparing(Comparator.comparingInt(SemanticMemoryEntry::importance).reversed())
                    .thenComparing(Comparator.comparingInt(SemanticMemoryEntry::confidence).reversed())
                    .thenComparing(Comparator.comparingInt(
                            (SemanticMemoryEntry entry) -> entry.sourceEventIds().size()
                    ).reversed())
                    .thenComparing(Comparator.comparingLong(SemanticMemoryEntry::gameTime).reversed())
                    .thenComparing(Comparator.comparingLong(
                            SemanticMemoryEntry::createdAtEpochMillis
                    ).reversed())
                    .thenComparing(entry -> entry.id().toString());
            candidates.sort(retentionOrder);
            candidates = new ArrayList<>(candidates.subList(0, safeMax));
        }

        candidates.sort(PERSISTENCE_ORDER);
        return List.copyOf(candidates);
    }

    static int durabilityScore(SemanticMemoryEntry entry) {
        if (entry == null) return 0;
        int provenanceContribution = switch (entry.provenance()) {
            case SYSTEM_OBSERVED -> 200;
            case PLAYER_TOLD -> 100;
            case NPC_TOLD -> 75;
            case INFERRED -> 25;
        };
        int sourceContribution = Math.min(entry.sourceEventIds().size(), 6) * 25;
        return entry.importance() * 4
                + entry.confidence() * 5 / 2
                + provenanceContribution
                + sourceContribution;
    }

    static long effectiveRetentionScore(SemanticMemoryEntry entry, long nowGameTime) {
        if (entry == null) return Long.MIN_VALUE;
        long safeNow = Math.max(0L, nowGameTime);
        long ageTicks = Math.max(0L, safeNow - entry.gameTime());
        return (long) durabilityScore(entry) * DECAY_STEP_TICKS - ageTicks;
    }
}
