package net.conczin.mca.livingworld.context;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalitySocialDialogueGuidanceRendererTest {
    @Test
    void neutralInfluenceAddsNoGuidance() {
        assertEquals(List.of(), PersonalitySocialDialogueGuidanceRenderer.render(PersonalitySocialInfluence.NEUTRAL));
        assertEquals(List.of(), PersonalitySocialDialogueGuidanceRenderer.render(null));
    }

    @Test
    void personalityOnlyInfluenceRendersOneBoundedToneLine() {
        List<String> lines = PersonalitySocialDialogueGuidanceRenderer.render(
                new PersonalitySocialInfluence(PersonalityDialogueStyle.WARM, NpcPairDisposition.NEUTRAL)
        );

        assertEquals(1, lines.size());
        assertEquals(
                "Dialogue style preference: warm and welcoming. This affects tone only; current-world facts, safety rules, permissions, and structured action validation take precedence.",
                lines.getFirst()
        );
    }

    @Test
    void pairOnlyInfluenceRendersOneBoundedInterpersonalLine() {
        List<String> lines = PersonalitySocialDialogueGuidanceRenderer.render(
                new PersonalitySocialInfluence(PersonalityDialogueStyle.NEUTRAL, NpcPairDisposition.DISTRUSTFUL)
        );

        assertEquals(1, lines.size());
        assertEquals(
                "Current counterpart stance: guarded and distrustful. This affects interpersonal tone only; it does not change factual truth, memory authority, or server action validation.",
                lines.getFirst()
        );
    }

    @Test
    void personalityAndPairInfluenceRemainFixedAtTwoLines() {
        List<String> lines = PersonalitySocialDialogueGuidanceRenderer.render(
                new PersonalitySocialInfluence(PersonalityDialogueStyle.CHEERFUL, NpcPairDisposition.AFFILIATIVE)
        );

        assertEquals(2, lines.size());
        assertTrue(lines.get(0).startsWith("Dialogue style preference:"));
        assertTrue(lines.get(1).startsWith("Current counterpart stance:"));
    }

    @Test
    void everyClosedNonNeutralStyleRendersExactlyOneServerAuthoredLine() {
        for (PersonalityDialogueStyle style : PersonalityDialogueStyle.values()) {
            List<String> lines = PersonalitySocialDialogueGuidanceRenderer.render(
                    new PersonalitySocialInfluence(style, NpcPairDisposition.NEUTRAL)
            );
            assertEquals(style == PersonalityDialogueStyle.NEUTRAL ? 0 : 1, lines.size(), style.name());
        }
    }

    @Test
    void everyClosedNonNeutralDispositionRendersExactlyOneServerAuthoredLine() {
        for (NpcPairDisposition disposition : NpcPairDisposition.values()) {
            List<String> lines = PersonalitySocialDialogueGuidanceRenderer.render(
                    new PersonalitySocialInfluence(PersonalityDialogueStyle.NEUTRAL, disposition)
            );
            assertEquals(disposition == NpcPairDisposition.NEUTRAL ? 0 : 1, lines.size(), disposition.name());
        }
    }

    @Test
    void rendererCannotLeakIdentifiersOrInventTruthAuthority() {
        String joined = String.join("\n", PersonalitySocialDialogueGuidanceRenderer.render(
                new PersonalitySocialInfluence(PersonalityDialogueStyle.CHARMING, NpcPairDisposition.FEARFUL)
        ));

        assertFalse(joined.contains("UUID"));
        assertFalse(joined.contains("trust="));
        assertFalse(joined.contains("respect="));
        assertFalse(joined.contains("fear="));
        assertFalse(joined.contains("affinity="));
        assertFalse(joined.contains("FACT"));
        assertFalse(joined.contains("BELIEF"));
        assertTrue(joined.contains("safety rules"));
        assertTrue(joined.contains("does not change factual truth"));
    }
}
