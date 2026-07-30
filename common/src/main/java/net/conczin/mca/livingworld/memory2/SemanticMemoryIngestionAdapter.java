package net.conczin.mca.livingworld.memory2;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Pure deterministic conversion from controlled evidence into typed semantic memory. */
public final class SemanticMemoryIngestionAdapter {
    static final int MAX_STATEMENT_CODE_POINTS = 240;
    private static final String FACT_ID_NAMESPACE = "semantic-fact-v1";
    private static final String BELIEF_ID_NAMESPACE = "semantic-belief-v1";

    private SemanticMemoryIngestionAdapter() {
    }

    public static Optional<SemanticMemoryEntry> toFact(MemoryEvent source) {
        if (!eligibleFact(source)) return Optional.empty();
        String statement = normalizeAndLimit(source.summary());
        if (statement.isBlank()) return Optional.empty();

        return Optional.of(new SemanticMemoryEntry(
                deterministicFactId(source.ownerNpcId(), source.id()),
                source.ownerNpcId(),
                SemanticMemoryEntry.Kind.FACT,
                statement,
                source.participants(),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                source.gameTime(),
                source.createdAtEpochMillis(),
                source.importance(),
                source.confidence(),
                List.of(source.id())
        ));
    }

    public static SemanticMemoryEntry toBelief(SemanticBeliefSource source) {
        if (source == null) throw new IllegalArgumentException("source is required");
        String statement = normalizeAndLimit(source.statement());
        if (statement.isBlank()) throw new IllegalArgumentException("statement is required");

        return new SemanticMemoryEntry(
                deterministicBeliefId(source, statement),
                source.ownerNpcId(),
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                source.relatedEntities(),
                source.provenance(),
                source.gameTime(),
                source.createdAtEpochMillis(),
                source.importance(),
                source.confidence(),
                source.sourceEventIds()
        );
    }

    private static boolean eligibleFact(MemoryEvent source) {
        if (source == null || source.provenance() != MemoryEvent.Provenance.SYSTEM_OBSERVED) return false;
        return source.type() == MemoryEvent.Type.ACTION
                || source.type() == MemoryEvent.Type.OBSERVATION
                || source.type() == MemoryEvent.Type.RELATIONSHIP_CHANGE;
    }

    private static UUID deterministicFactId(UUID ownerNpcId, UUID sourceEventId) {
        String canonical = FACT_ID_NAMESPACE
                + '\n' + ownerNpcId
                + '\n' + sourceEventId;
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    private static UUID deterministicBeliefId(SemanticBeliefSource source, String normalizedStatement) {
        StringBuilder canonical = new StringBuilder(BELIEF_ID_NAMESPACE)
                .append('\n').append(source.ownerNpcId())
                .append('\n').append(source.provenance())
                .append('\n').append(normalizedStatement);
        source.sourceEventIds().stream()
                .sorted(Comparator.comparing(UUID::toString))
                .forEach(id -> canonical.append('\n').append(id));
        return UUID.nameUUIDFromBytes(canonical.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizeAndLimit(String value) {
        String normalized = normalizeWhitespace(value);
        if (normalized.codePointCount(0, normalized.length()) <= MAX_STATEMENT_CODE_POINTS) return normalized;
        int end = normalized.offsetByCodePoints(0, MAX_STATEMENT_CODE_POINTS);
        return normalized.substring(0, end);
    }

    private static String normalizeWhitespace(String value) {
        if (value == null || value.isBlank()) return "";
        StringBuilder output = new StringBuilder(value.length());
        boolean previousWhitespace = false;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
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
}
