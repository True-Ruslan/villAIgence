package net.conczin.mca.livingworld.knowledge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcActionEventFormatterTest {
    @Test
    void formatsOnlyKnownSafeActions() {
        assertEquals(
                "Ada started following Ruslan.",
                NpcActionEventFormatter.describe("follow-player", "Ada", "Ruslan").orElseThrow()
        );
        assertEquals(
                "Ada opened trade with Ruslan.",
                NpcActionEventFormatter.describe("open-trade-window", "Ada", "Ruslan").orElseThrow()
        );
        assertTrue(NpcActionEventFormatter.describe("run-server-command", "Ada", "Ruslan").isEmpty());
    }

    @Test
    void sanitizesControlCharactersAndWhitespaceInNames() {
        assertEquals(
                "Ada Smith started following Ruslan Test.",
                NpcActionEventFormatter.describe("follow-player", "Ada\n  Smith", "Ruslan\tTest").orElseThrow()
        );
    }
}
