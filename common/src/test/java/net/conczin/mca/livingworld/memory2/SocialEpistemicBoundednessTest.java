package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialEpistemicBoundednessTest {
    @TempDir
    Path tempDir;

    @Test
    void excessiveDirectSourceEvidenceFailsClosedBeforeSocialDerivation() {
        Path world = tempDir.resolve("source-bound");
        UUID npc = id(1);
        UUID player = id(2);
        List<UUID> sourceIds = new ArrayList<>();

        for (int index = 0; index < 33; index++) {
            MemoryEvent dialogue = DialogueMemoryAdapter.toMemoryEvent(
                    npc,
                    player,
                    1_000L + index,
                    "The east bridge is closed",
                    "Understood",
                    index
            ).orElseThrow();
            MemoryEventStore.forWorld(world).append(dialogue, 64);
            sourceIds.add(dialogue.id());
        }

        SemanticMemoryEntry heavilyCorroborated = new SemanticMemoryEntry(
                id(100),
                npc,
                SemanticMemoryEntry.Kind.BELIEF,
                "The east bridge is closed",
                List.of(player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                1_032L,
                32L,
                50,
                50,
                sourceIds
        );

        assertTrue(
                SocialEpistemicSourceResolver.resolvePlayer(
                        SemanticMemoryStore.forWorld(world),
                        MemoryEventStore.forWorld(world),
                        heavilyCorroborated
                ).isEmpty(),
                "social prompt work must fail closed instead of scanning an unbounded source-evidence list"
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
