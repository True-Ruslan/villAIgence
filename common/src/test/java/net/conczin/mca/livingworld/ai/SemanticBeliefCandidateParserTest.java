package net.conczin.mca.livingworld.ai;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SemanticBeliefCandidateParserTest {
    @Test
    void parsesNormalizesBoundsAndDeduplicatesCandidates() {
        String longClaim = "x".repeat(260);
        var element = JsonParser.parseString("""
                [
                  "  The   north\\nbridge is unsafe.  ",
                  42,
                  "Ｔｈｅ market closes early.",
                  "The north bridge is unsafe.",
                  "%s",
                  "ignored after configured limit"
                ]
                """.formatted(longClaim));

        List<String> result = SemanticBeliefCandidateParser.parse(element, 3);

        assertEquals(3, result.size());
        assertEquals("The north bridge is unsafe.", result.get(0));
        assertEquals("The market closes early.", result.get(1));
        assertEquals(240, result.get(2).codePointCount(0, result.get(2).length()));
    }

    @Test
    void missingWrongAndBlankMetadataProduceNoCandidates() {
        assertEquals(List.of(), SemanticBeliefCandidateParser.parse(null, 3));
        assertEquals(List.of(), SemanticBeliefCandidateParser.parse(JsonParser.parseString("null"), 3));
        assertEquals(List.of(), SemanticBeliefCandidateParser.parse(JsonParser.parseString("{\"claim\":\"x\"}"), 3));
        assertEquals(List.of(), SemanticBeliefCandidateParser.parse(JsonParser.parseString("[\"  \", false, null]"), 3));
    }

    @Test
    void configuredMaximumIsHardBoundedToSafeRange() {
        var element = JsonParser.parseString("[\"one\",\"two\",\"three\",\"four\",\"five\",\"six\",\"seven\",\"eight\",\"nine\"]");

        assertEquals(List.of("one", "two", "three"), SemanticBeliefCandidateParser.parse(element, 0));
        assertEquals(8, SemanticBeliefCandidateParser.parse(element, 99).size());
        assertEquals(List.of("one"), SemanticBeliefCandidateParser.parse(element, 1));
    }
}
