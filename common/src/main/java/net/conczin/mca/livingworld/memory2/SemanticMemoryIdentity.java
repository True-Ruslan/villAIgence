package net.conczin.mca.livingworld.memory2;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Shared deterministic identity for one logical Semantic Memory claim. */
final class SemanticMemoryIdentity {
    private static final String LOGICAL_ID_NAMESPACE = "semantic-logical-claim-v1";
    private static final Comparator<UUID> UUID_ORDER = Comparator.comparing(UUID::toString);

    private SemanticMemoryIdentity() {
    }

    static LogicalClaimKey key(SemanticMemoryEntry entry) {
        if (entry == null) throw new IllegalArgumentException("entry is required");
        return new LogicalClaimKey(
                entry.ownerNpcId(),
                entry.kind(),
                entry.provenance(),
                canonicalStatement(entry.statement()),
                canonicalIds(entry.relatedEntities())
        );
    }

    static UUID logicalClaimId(SemanticMemoryEntry entry) {
        LogicalClaimKey key = key(entry);
        StringBuilder canonical = new StringBuilder(LOGICAL_ID_NAMESPACE)
                .append('\n').append(key.ownerNpcId())
                .append('\n').append(key.kind())
                .append('\n').append(key.provenance())
                .append('\n').append(key.statement());
        for (UUID relatedEntity : key.relatedEntities()) {
            canonical.append('\n').append(relatedEntity);
        }
        return UUID.nameUUIDFromBytes(canonical.toString().getBytes(StandardCharsets.UTF_8));
    }

    static String canonicalStatement(String value) {
        return normalizeDisplay(value).toLowerCase(Locale.ROOT);
    }

    static String normalizeDisplay(String value) {
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

    static List<UUID> canonicalIds(List<UUID> values) {
        if (values == null || values.isEmpty()) return List.of();
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID value : values) {
            if (value != null) unique.add(value);
        }
        List<UUID> sorted = new ArrayList<>(unique);
        sorted.sort(UUID_ORDER);
        return List.copyOf(sorted);
    }

    record LogicalClaimKey(
            UUID ownerNpcId,
            SemanticMemoryEntry.Kind kind,
            MemoryEvent.Provenance provenance,
            String statement,
            List<UUID> relatedEntities
    ) {
        LogicalClaimKey {
            if (ownerNpcId == null) throw new IllegalArgumentException("ownerNpcId is required");
            if (kind == null) throw new IllegalArgumentException("kind is required");
            if (provenance == null) throw new IllegalArgumentException("provenance is required");
            if (statement == null || statement.isBlank()) throw new IllegalArgumentException("statement is required");
            relatedEntities = canonicalIds(relatedEntities);
        }
    }
}
