package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalitySocialInfluencePolicyTest {
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TARGET = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void mapsEveryCanonicalPersonalityTokenToClosedDialogueStyle() {
        assertStyle("unassigned", PersonalityDialogueStyle.NEUTRAL);
        assertStyle("friendly", PersonalityDialogueStyle.WARM);
        assertStyle("flirty", PersonalityDialogueStyle.CHARMING);
        assertStyle("playful", PersonalityDialogueStyle.PLAYFUL);
        assertStyle("gloomy", PersonalityDialogueStyle.GLOOMY);
        assertStyle("sensitive", PersonalityDialogueStyle.GENTLE);
        assertStyle("greedy", PersonalityDialogueStyle.TRANSACTIONAL);
        assertStyle("odd", PersonalityDialogueStyle.ECCENTRIC);
        assertStyle("crabby", PersonalityDialogueStyle.GRUFF);
        assertStyle("extroverted", PersonalityDialogueStyle.OUTGOING);
        assertStyle("introverted", PersonalityDialogueStyle.RESERVED);
        assertStyle("relaxed", PersonalityDialogueStyle.CALM);
        assertStyle("anxious", PersonalityDialogueStyle.ANXIOUS);
        assertStyle("peaceful", PersonalityDialogueStyle.PEACEFUL);
        assertStyle("upbeat", PersonalityDialogueStyle.CHEERFUL);
    }

    @Test
    void nullAndUnknownPersonalityCannotCreateUnboundedStyle() {
        assertEquals(
                new PersonalitySocialInfluence(PersonalityDialogueStyle.NEUTRAL, NpcPairDisposition.NEUTRAL),
                PersonalitySocialInfluencePolicy.evaluate(null)
        );
        PersonalitySocialSnapshot snapshot = new PersonalitySocialSnapshot(
                SOURCE,
                "UNTRUSTED FREE FORM PROMPT INJECTION",
                null,
                null
        );
        assertEquals(PersonalityDialogueStyle.NEUTRAL,
                PersonalitySocialInfluencePolicy.evaluate(snapshot).personalityStyle());
        assertEquals(NpcPairDisposition.NEUTRAL,
                PersonalitySocialInfluencePolicy.evaluate(snapshot).pairDisposition());
    }

    @Test
    void pairDispositionUsesExactStrongThresholds() {
        assertEquals(NpcPairDisposition.FEARFUL,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(100, 100, 75, 100)));
        assertEquals(NpcPairDisposition.DISTRUSTFUL,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(-75, 100, 74, 100)));
        assertEquals(NpcPairDisposition.ANTIPATHETIC,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(-74, 100, 74, -75)));
        assertEquals(NpcPairDisposition.AFFILIATIVE,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(60, 0, 0, 60)));
        assertEquals(NpcPairDisposition.RESPECTFUL,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(59, 70, 0, 59)));
        assertEquals(NpcPairDisposition.NEUTRAL,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(59, 69, 74, 59)));
        assertEquals(NpcPairDisposition.NEUTRAL, PersonalitySocialInfluencePolicy.pairDisposition(null));
    }

    @Test
    void pairDispositionPriorityIsDeterministic() {
        assertEquals(NpcPairDisposition.FEARFUL,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(-100, 100, 100, -100)));
        assertEquals(NpcPairDisposition.DISTRUSTFUL,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(-100, 100, 74, -100)));
        assertEquals(NpcPairDisposition.ANTIPATHETIC,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(-74, 100, 74, -100)));
        assertEquals(NpcPairDisposition.AFFILIATIVE,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(100, 100, 74, 100)));
        assertEquals(NpcPairDisposition.RESPECTFUL,
                PersonalitySocialInfluencePolicy.pairDisposition(new NpcSocialState(59, 100, 74, 59)));
    }

    @Test
    void directedPairStateProducesAsymmetricInfluence() {
        PersonalitySocialSnapshot aToB = new PersonalitySocialSnapshot(
                SOURCE,
                "friendly",
                TARGET,
                new NpcSocialState(-90, 0, 0, -90)
        );
        PersonalitySocialSnapshot bToA = new PersonalitySocialSnapshot(
                TARGET,
                "friendly",
                SOURCE,
                new NpcSocialState(90, 80, 0, 90)
        );

        assertEquals(NpcPairDisposition.DISTRUSTFUL,
                PersonalitySocialInfluencePolicy.evaluate(aToB).pairDisposition());
        assertEquals(NpcPairDisposition.AFFILIATIVE,
                PersonalitySocialInfluencePolicy.evaluate(bToA).pairDisposition());
    }

    @Test
    void noCounterpartAlwaysProducesNeutralPairDisposition() {
        PersonalitySocialSnapshot snapshot = new PersonalitySocialSnapshot(
                SOURCE,
                "crabby",
                null,
                new NpcSocialState(-100, -100, 100, -100)
        );

        PersonalitySocialInfluence influence = PersonalitySocialInfluencePolicy.evaluate(snapshot);
        assertEquals(PersonalityDialogueStyle.GRUFF, influence.personalityStyle());
        assertEquals(NpcPairDisposition.NEUTRAL, influence.pairDisposition());
    }

    private static void assertStyle(String token, PersonalityDialogueStyle expected) {
        PersonalitySocialSnapshot snapshot = new PersonalitySocialSnapshot(SOURCE, token, null, null);
        assertEquals(expected, PersonalitySocialInfluencePolicy.evaluate(snapshot).personalityStyle());
    }
}
