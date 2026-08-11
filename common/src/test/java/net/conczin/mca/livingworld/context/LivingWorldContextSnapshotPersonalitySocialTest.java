package net.conczin.mca.livingworld.context;

import net.conczin.mca.livingworld.relationship.NpcSocialState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LivingWorldContextSnapshotPersonalitySocialTest {
    @TempDir
    Path tempDir;

    private static final UUID PLAYER = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID VILLAGER = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID COUNTERPART = UUID.fromString("10000000-0000-0000-0000-000000000003");

    @Test
    void previousFullConstructorGetsBoundedCompatibilitySnapshot() {
        LivingWorldContextSnapshot snapshot = new LivingWorldContextSnapshot(
                PLAYER,
                VILLAGER,
                "Player",
                "Villager",
                List.of("base"),
                List.of("fact"),
                List.of("lore"),
                List.of("episodic"),
                List.of("semantic"),
                List.of("contradiction"),
                List.of(),
                42L,
                100L,
                tempDir,
                false,
                false,
                "en"
        );

        PersonalitySocialSnapshot personalitySocial = snapshot.personalitySocialSnapshot();
        assertEquals(VILLAGER, personalitySocial.sourceNpcId());
        assertEquals("unassigned", personalitySocial.personalityToken());
        assertFalse(personalitySocial.hasCounterpart());
        assertEquals(NpcSocialState.NEUTRAL, personalitySocial.directedSocialState());
    }

    @Test
    void canonicalConstructorPreservesExplicitTypedSnapshot() {
        PersonalitySocialSnapshot personalitySocial = new PersonalitySocialSnapshot(
                VILLAGER,
                "friendly",
                COUNTERPART,
                new NpcSocialState(12, 4, 1, 8)
        );

        LivingWorldContextSnapshot snapshot = new LivingWorldContextSnapshot(
                PLAYER,
                VILLAGER,
                "Player",
                "Villager",
                List.of("base"),
                List.of("fact"),
                personalitySocial,
                List.of("lore"),
                List.of("episodic"),
                List.of("semantic"),
                List.of("contradiction"),
                List.of(),
                42L,
                100L,
                tempDir,
                false,
                false,
                "en"
        );

        assertSame(personalitySocial, snapshot.personalitySocialSnapshot());
    }

    @Test
    void canonicalConstructorRejectsPersonalitySocialSnapshotOwnedByAnotherNpc() {
        UUID otherNpc = UUID.fromString("10000000-0000-0000-0000-000000000099");
        PersonalitySocialSnapshot foreignSnapshot = new PersonalitySocialSnapshot(
                otherNpc,
                "friendly",
                COUNTERPART,
                new NpcSocialState(12, 4, 1, 8)
        );

        assertThrows(IllegalArgumentException.class, () -> new LivingWorldContextSnapshot(
                PLAYER,
                VILLAGER,
                "Player",
                "Villager",
                List.of("base"),
                List.of("fact"),
                foreignSnapshot,
                List.of("lore"),
                List.of("episodic"),
                List.of("semantic"),
                List.of("contradiction"),
                List.of(),
                42L,
                100L,
                tempDir,
                false,
                false,
                "en"
        ));
    }
}
