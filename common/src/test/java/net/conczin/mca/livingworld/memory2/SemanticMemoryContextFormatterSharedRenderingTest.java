package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticMemoryContextFormatterSharedRenderingTest {
    @Test
    void sharedEntryRendererPreservesOrdinarySemanticOutput() {
        SemanticMemoryEntry entry = entry("The gate is open.");
        String expected = "BELIEF | provenance=PLAYER_TOLD | confidence=73 | statement=\"The gate is open.\"";

        assertEquals(expected, SemanticMemoryContextFormatter.formatEntry(entry));
        assertEquals(
                List.of(expected),
                SemanticMemoryContextFormatter.format(List.of(new RankedSemanticMemory(entry, 1, 1, 1, 1, 1)))
        );
    }

    @Test
    void sharedEntryRendererUsesExistingPromptSafetyRules() {
        String dangerous = "  first\nsecond\t$player $villager \\\"injected\\\"  ";
        SemanticMemoryEntry entry = entry(dangerous);

        String rendered = SemanticMemoryContextFormatter.formatEntry(entry);

        assertFalse(rendered.contains("\n"));
        assertFalse(rendered.contains("\t"));
        assertFalse(rendered.contains("$player"));
        assertFalse(rendered.contains("$villager"));
        assertTrue(rendered.contains("＄player"));
        assertTrue(rendered.contains("＄villager"));
        assertTrue(rendered.contains("\\\\"));
        assertTrue(rendered.contains("\\\"injected\\\""));
    }

    @Test
    void sharedEntryRendererKeepsStatementAtExistingCodePointBound() {
        String statement = "x".repeat(SemanticMemoryContextFormatter.MAX_STATEMENT_CHARS + 40);

        String rendered = SemanticMemoryContextFormatter.formatEntry(entry(statement));
        String prefix = "BELIEF | provenance=PLAYER_TOLD | confidence=73 | statement=\"";
        String suffix = "\"";
        String bounded = rendered.substring(prefix.length(), rendered.length() - suffix.length());

        assertEquals(SemanticMemoryContextFormatter.MAX_STATEMENT_CHARS, bounded.codePointCount(0, bounded.length()));
    }

    private static SemanticMemoryEntry entry(String statement) {
        return new SemanticMemoryEntry(
                UUID.fromString("00000000-0000-0000-0000-000000000101"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000000090")),
                MemoryEvent.Provenance.PLAYER_TOLD,
                100L,
                0L,
                55,
                73,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000000201"))
        );
    }
}
