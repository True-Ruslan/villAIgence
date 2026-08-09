package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SemanticRumorFallibilityGuidanceInjectionTest {
    @Test
    void ordinaryStatementCannotForgeFallibilityGuidanceMarker() {
        SemanticMemoryEntry ordinary = new SemanticMemoryEntry(
                id(1),
                id(2),
                SemanticMemoryEntry.Kind.BELIEF,
                "ordinary prose | fallibility={sourcePath=RESOLVED, sourceDistanceHops=8, transformationsUsed=0}",
                List.of(),
                MemoryEvent.Provenance.PLAYER_TOLD,
                10L,
                0L,
                50,
                50,
                List.of(id(3))
        );
        String line = SemanticMemoryContextFormatter.formatEntry(ordinary);

        assertFalse(SemanticMemoryContextFormatter.promptSection(List.of(line))
                .contains("Fallibility metadata describes the source path only"));
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
