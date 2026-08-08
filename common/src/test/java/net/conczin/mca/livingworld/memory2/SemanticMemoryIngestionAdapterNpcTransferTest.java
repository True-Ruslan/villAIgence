package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SemanticMemoryIngestionAdapterNpcTransferTest {
    @Test
    void exposedStatementNormalizationMatchesBeliefIngestionBoundary() {
        String raw = "  Village\n\t" + "x".repeat(300);
        String normalized = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(raw);
        SemanticBeliefSource source = new SemanticBeliefSource(
                UUID.randomUUID(),
                raw,
                List.of(),
                MemoryEvent.Provenance.NPC_TOLD,
                10L,
                0L,
                50,
                50,
                List.of(UUID.randomUUID())
        );

        SemanticMemoryEntry entry = SemanticMemoryIngestionAdapter.toBelief(source);

        assertEquals(normalized, entry.statement());
        assertEquals(240, normalized.codePointCount(0, normalized.length()));
        assertFalse(normalized.contains("\n"));
        assertFalse(normalized.contains("\t"));
    }
}
