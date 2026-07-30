package net.conczin.mca.livingworld.memory2;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

        ConsolidationKey key = key(first);
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
        Map<ConsolidationKey, SemanticMemoryEntry> sourced = new LinkedHashMap<>();
        List<SemanticMemoryEntry> unsourced = new ArrayList<>();

        for (SemanticMemoryEntry entry : entries) {
            if (entry == null || !seenEntryIds.add(entry.id())) continue;
            if (entry.sourceEventIds().isEmpty()) {
                unsourced.add(entry);
                continue;
            }

            ConsolidationKey key = key(entry);
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
                && key(first).equals(key(second));
    }

    private static ConsolidationKey key(SemanticMemoryEntry entry) {
        return new ConsolidationKey(
                entry.ownerNpcId(),
                entry.kind(),
                entry.provenance(),
                canonicalStatement(entry.statement()),
                sortedIds(entry.relatedEntities())
        );
    }

    private static UUID deterministicId(ConsolidationKey key) {
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
        String firstDisplay = normalizeDisplay(first);
        String secondDisplay = normalizeDisplay(second);
        return firstDisplay.compareTo(secondDisplay) <= 0 ? firstDisplay : secondDisplay;
    }

    private static String canonicalStatement(String value) {
        return normalizeDisplay(value).toLowerCase(Locale.ROOT);
    }

    private static String normalizeDisplay(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC);
        StringBuilder output = new StringBuilder(normalized.length());
        boolean previousWhitespace = false;
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
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

    private static List<UUID> sortedUnion(List<UUID> first, List<UUID> second) {
        Set<UUID> values = new LinkedHashSet<>();
        if (first != null) values.addAll(first);
        if (second != null) values.addAll(second);
        values.remove(null);
        List<UUID> sorted = new ArrayList<>(values);
        sorted.sort(UUID_ORDER);
        return List.copyOf(sorted);
    }

    private static List<UUID> sortedIds(List<UUID> values) {
        if (values == null || values.isEmpty()) return List.of();
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID value : values) {
            if (value != null) unique.add(value);
        }
        List<UUID> sorted = new ArrayList<>(unique);
        sorted.sort(UUID_ORDER);
        return List.copyOf(sorted);
    }

    private record ConsolidationKey(
            UUID ownerNpcId,
            SemanticMemoryEntry.Kind kind,
            MemoryEvent.Provenance provenance,
            String statement,
            List<UUID> relatedEntities
    ) {
    }
}
