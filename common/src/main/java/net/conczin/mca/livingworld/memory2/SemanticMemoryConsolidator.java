package net.conczin.mca.livingworld.memory2;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Pure provider-independent consolidation of logically identical typed semantic entries. */
public final class SemanticMemoryConsolidator {
    private static final String CONSOLIDATED_ID_NAMESPACE = "semantic-consolidated-v1";
    private static final Comparator<UUID> UUID_ORDER = Comparator.comparing(UUID::toString);

    private SemanticMemoryConsolidator() {
    }

    public static Optional<SemanticMemoryEntry> merge(
            SemanticMemoryEntry first,
            SemanticMemoryEntry second
    ) {
        if (!compatible(first, second)) return Optional.empty();

        SemanticMemoryIdentity.LogicalClaimKey key = SemanticMemoryIdentity.key(first);
        List<UUID> sources = sortedUnion(first.sourceEventIds(), second.sourceEventIds());
        List<UUID> relatedEntities = sortedUnion(first.relatedEntities(), second.relatedEntities());
        String statement = deterministicStatement(first.statement(), second.statement());

        return Optional.of(new SemanticMemoryEntry(
                deterministicId(key),
                first.ownerNpcId(),
                first.kind(),
                statement,
                relatedEntities,
                first.provenance(),
                Math.max(first.gameTime(), second.gameTime()),
                Math.max(first.createdAtEpochMillis(), second.createdAtEpochMillis()),
                Math.max(first.importance(), second.importance()),
                Math.max(first.confidence(), second.confidence()),
                sources
        ));
    }

    public static List<SemanticMemoryEntry> consolidateAll(List<SemanticMemoryEntry> entries) {
        if (entries == null || entries.isEmpty()) return List.of();

        Set<UUID> seenEntryIds = new LinkedHashSet<>();
        Map<SemanticMemoryIdentity.LogicalClaimKey, SemanticMemoryEntry> sourced = new LinkedHashMap<>();
        List<SemanticMemoryEntry> unsourced = new ArrayList<>();

        for (SemanticMemoryEntry entry : entries) {
            if (entry == null || !seenEntryIds.add(entry.id())) continue;
            if (entry.sourceEventIds().isEmpty()) {
                unsourced.add(entry);
                continue;
            }

            SemanticMemoryIdentity.LogicalClaimKey key = SemanticMemoryIdentity.key(entry);
            SemanticMemoryEntry existing = sourced.get(key);
            if (existing == null) {
                sourced.put(key, entry);
            } else {
                sourced.put(key, merge(existing, entry).orElseThrow());
            }
        }

        List<SemanticMemoryEntry> result = new ArrayList<>(sourced.size() + unsourced.size());
        result.addAll(sourced.values());
        result.addAll(unsourced);
        return List.copyOf(result);
    }

    static boolean compatible(SemanticMemoryEntry first, SemanticMemoryEntry second) {
        return first != null
                && second != null
                && !first.sourceEventIds().isEmpty()
                && !second.sourceEventIds().isEmpty()
                && SemanticMemoryIdentity.key(first).equals(SemanticMemoryIdentity.key(second));
    }

    private static UUID deterministicId(SemanticMemoryIdentity.LogicalClaimKey key) {
        StringBuilder canonical = new StringBuilder(CONSOLIDATED_ID_NAMESPACE)
                .append('\n').append(key.ownerNpcId())
                .append('\n').append(key.kind())
                .append('\n').append(key.provenance())
                .append('\n').append(key.statement());
        for (UUID relatedEntity : key.relatedEntities()) {
            canonical.append('\n').append(relatedEntity);
        }
        return UUID.nameUUIDFromBytes(canonical.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String deterministicStatement(String first, String second) {
        String firstDisplay = SemanticMemoryIdentity.normalizeDisplay(first);
        String secondDisplay = SemanticMemoryIdentity.normalizeDisplay(second);
        return firstDisplay.compareTo(secondDisplay) <= 0 ? firstDisplay : secondDisplay;
    }

    private static List<UUID> sortedUnion(List<UUID> first, List<UUID> second) {
        Set<UUID> values = new LinkedHashSet<>();
        if (first != null) values.addAll(first);
        if (second != null) values.addAll(second);
        values.remove(null);
        List<UUID> sorted = new ArrayList<>(values);
        sorted.sort(UUID_ORDER);
        return List.copyOf(sorted);
    }
}
