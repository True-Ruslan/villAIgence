package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticOppositionClassifierTest {
    private static final UUID NPC = id(1);
    private static final UUID PLAYER = id(90);

    @Test
    void recognizesExactlyOneStandaloneEnglishNegationSymmetrically() {
        SemanticMemoryEntry positive = belief(id(10), "The gate is open");
        SemanticMemoryEntry negative = belief(id(11), "The gate is not open");

        assertTrue(SemanticOppositionClassifier.opposes(positive, negative));
        assertTrue(SemanticOppositionClassifier.opposes(negative, positive));
    }

    @Test
    void recognizesExactlyOneStandaloneRussianNegationSymmetrically() {
        SemanticMemoryEntry positive = belief(id(20), "Ворота открыты");
        SemanticMemoryEntry negative = belief(id(21), "Ворота не открыты");

        assertTrue(SemanticOppositionClassifier.opposes(positive, negative));
        assertTrue(SemanticOppositionClassifier.opposes(negative, positive));
    }

    @Test
    void rejectsEquivalentDoubleNegationAntonymsNumbersAndReorderedStatements() {
        assertFalse(SemanticOppositionClassifier.opposes(
                belief(id(30), "The gate is open"),
                belief(id(31), "  THE   GATE IS OPEN ")
        ));
        assertFalse(SemanticOppositionClassifier.opposes(
                belief(id(32), "The gate is open"),
                belief(id(33), "The gate is not not open")
        ));
        assertFalse(SemanticOppositionClassifier.opposes(
                belief(id(34), "The gate is open"),
                belief(id(35), "The gate is closed")
        ));
        assertFalse(SemanticOppositionClassifier.opposes(
                belief(id(36), "There are 4 guards"),
                belief(id(37), "There are 5 guards")
        ));
        assertFalse(SemanticOppositionClassifier.opposes(
                belief(id(38), "The gate is open"),
                belief(id(39), "Open is the gate not")
        ));
    }

    @Test
    void trailingSentenceOmissionAloneIsNotOpposition() {
        SemanticMemoryEntry source = belief(id(40), "The gate is open. A guard is nearby.");
        SemanticMemoryEntry transformed = belief(id(41), "The gate is open.");

        assertFalse(SemanticOppositionClassifier.opposes(source, transformed));
        assertFalse(SemanticOppositionClassifier.opposes(transformed, source));
    }

    @Test
    void embeddedNegationTextIsNotAStandaloneNegationToken() {
        assertFalse(SemanticOppositionClassifier.opposes(
                belief(id(50), "The notebook is here"),
                belief(id(51), "The book is here")
        ));
        assertFalse(SemanticOppositionClassifier.opposes(
                belief(id(52), "Небо ясное"),
                belief(id(53), "Бо ясное")
        ));
    }

    private static SemanticMemoryEntry belief(UUID entryId, String statement) {
        return new SemanticMemoryEntry(
                entryId,
                NPC,
                SemanticMemoryEntry.Kind.BELIEF,
                statement,
                List.of(PLAYER),
                MemoryEvent.Provenance.NPC_TOLD,
                100L,
                0L,
                50,
                50,
                List.of(id(900))
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
