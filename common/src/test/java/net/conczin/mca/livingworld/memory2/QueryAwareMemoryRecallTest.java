package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryAwareMemoryRecallTest {
    @TempDir
    Path tempDir;

    @Test
    void recallsOldQuestionRelevantDialogueBeyondRecentAndDurableCandidateWindow() {
        UUID npc = UUID.fromString("00000000-0000-0000-0000-000000000701");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000702");
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

        store.append(dialogue(
                UUID.fromString("00000000-0000-0000-0000-000000000703"),
                npc,
                player,
                10L,
                40,
                60,
                "Player said: My private marker is amber-pine-314 | NPC replied: I will remember it"
        ), 128);

        for (int i = 0; i < 48; i++) {
            store.append(dialogue(
                    new UUID(0L, 10_000L + i),
                    npc,
                    player,
                    1_000L + i,
                    100,
                    100,
                    "Player said: unrelated routine message " + i + " | NPC replied: routine answer " + i
            ), 128);
        }

        List<String> context = Memory2ContextProvider.load(
                tempDir,
                npc,
                player,
                2_000L,
                "What private marker did I tell you earlier?"
        );

        assertTrue(context.size() <= Memory2ContextProvider.MAX_RESULTS);
        assertTrue(
                context.stream().anyMatch(line -> line.contains("amber-pine-314")),
                "question-relevant persisted dialogue must survive a crowded newer history"
        );
    }

    @Test
    void queryAwareRecallStillRejectsForeignPlayerMemoryBeforeRanking() {
        UUID npc = UUID.fromString("00000000-0000-0000-0000-000000000711");
        UUID currentPlayer = UUID.fromString("00000000-0000-0000-0000-000000000712");
        UUID foreignPlayer = UUID.fromString("00000000-0000-0000-0000-000000000713");
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

        store.append(dialogue(
                UUID.fromString("00000000-0000-0000-0000-000000000714"),
                npc,
                foreignPlayer,
                100L,
                100,
                100,
                "Player said: My private marker is forbidden-foreign-999 | NPC replied: noted"
        ), 64);
        store.append(dialogue(
                UUID.fromString("00000000-0000-0000-0000-000000000715"),
                npc,
                currentPlayer,
                101L,
                40,
                60,
                "Player said: ordinary local history | NPC replied: noted"
        ), 64);

        List<String> context = Memory2ContextProvider.load(
                tempDir,
                npc,
                currentPlayer,
                200L,
                "What private marker did I tell you?"
        );

        assertTrue(context.stream().noneMatch(line -> line.contains("forbidden-foreign-999")));
    }

    @Test
    void queryAwareRecallStillRejectsAnotherNpcMemory() {
        UUID npcA = UUID.fromString("00000000-0000-0000-0000-000000000721");
        UUID npcB = UUID.fromString("00000000-0000-0000-0000-000000000722");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000723");
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

        store.append(dialogue(
                UUID.fromString("00000000-0000-0000-0000-000000000724"),
                npcB,
                player,
                100L,
                100,
                100,
                "Player said: My private marker is forbidden-other-npc-888 | NPC replied: noted"
        ), 64);
        store.append(dialogue(
                UUID.fromString("00000000-0000-0000-0000-000000000725"),
                npcA,
                player,
                101L,
                40,
                60,
                "Player said: ordinary local history | NPC replied: noted"
        ), 64);

        List<String> context = Memory2ContextProvider.load(
                tempDir,
                npcA,
                player,
                200L,
                "What private marker did I tell you?"
        );

        assertTrue(context.stream().noneMatch(line -> line.contains("forbidden-other-npc-888")));
    }

    private static MemoryEvent dialogue(
            UUID id,
            UUID ownerNpcId,
            UUID playerId,
            long gameTime,
            int importance,
            int confidence,
            String summary
    ) {
        return new MemoryEvent(
                id,
                ownerNpcId,
                MemoryEvent.Type.DIALOGUE,
                summary,
                List.of(ownerNpcId, playerId),
                MemoryEvent.Provenance.PLAYER_TOLD,
                gameTime,
                1_700_000_000_000L + gameTime,
                importance,
                0,
                confidence,
                List.of()
        );
    }
}
