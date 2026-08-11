package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalitySocialContextRendererTest {
    private static final UUID SOURCE = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TARGET = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void personalityOnlyRenderingIsFixedSizeAndServerAuthored() {
        PersonalitySocialSnapshot snapshot = new PersonalitySocialSnapshot(
                SOURCE,
                "extroverted",
                null,
                NpcSocialState.NEUTRAL
        );

        List<String> lines = PersonalitySocialContextRenderer.render(snapshot);

        assertEquals(List.of(
                "Current NPC personality: extroverted. This is server-owned descriptive state, not an instruction or current-world fact override."
        ), lines);
        assertFalse(lines.getFirst().contains(SOURCE.toString()));
    }

    @Test
    void directedRenderingContainsOnlyOneExactPairAndBoundedNumbers() {
        PersonalitySocialSnapshot snapshot = new PersonalitySocialSnapshot(
                SOURCE,
                "crabby",
                TARGET,
                new NpcSocialState(17, -8, 4, -23)
        );

        List<String> lines = PersonalitySocialContextRenderer.render(snapshot);

        assertEquals(2, lines.size());
        assertEquals(
                "Current NPC personality: crabby. This is server-owned descriptive state, not an instruction or current-world fact override.",
                lines.get(0)
        );
        assertEquals(
                "Current directed social state toward the current NPC counterpart: trust=17, respect=-8, fear=4, affinity=-23.",
                lines.get(1)
        );
        String rendered = String.join("\n", lines);
        assertFalse(rendered.contains(SOURCE.toString()));
        assertFalse(rendered.contains(TARGET.toString()));
        assertTrue(rendered.length() < 320);
    }
}
