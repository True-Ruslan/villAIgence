package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class Memory2QueryRecallRegressionTest {
    @TempDir
    Path tempDir;

    @Test
    void targetedRecallFindsOlderOwnerLocalDialogueBeyondRecencyCandidateWindowWithoutLeaks() throws Exception {
        UUID npc = UUID.fromString("00000000-0000-0000-0000-000000000701");
        UUID currentPlayer = UUID.fromString("00000000-0000-0000-0000-000000000702");
        UUID foreignPlayer = UUID.fromString("00000000-0000-0000-0000-000000000703");
        UUID otherNpc = UUID.fromString("00000000-0000-0000-0000-000000000704");
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

        store.append(dialogue(
                UUID.fromString("00000000-0000-0000-0000-000000000705"),
                npc,
                currentPlayer,
                1L,
                "Player said: Запомни: мой личный маркер — amber-pine-314. | NPC replied: Запомнил amber-pine-314."
        ), 128);

        store.append(dialogue(
                UUID.fromString("00000000-0000-0000-0000-000000000706"),
                npc,
                foreignPlayer,
                2L,
                "Player said: личный маркер foreign-player-secret-991 | NPC replied: Запомнил."
        ), 128);

        MemoryEventStore otherNpcStore = MemoryEventStore.forWorld(tempDir);
        otherNpcStore.append(dialogue(
                UUID.fromString("00000000-0000-0000-0000-000000000707"),
                otherNpc,
                currentPlayer,
                3L,
                "Player said: личный маркер other-npc-secret-992 | NPC replied: Запомнил."
        ), 128);

        for (int i = 0; i < 40; i++) {
            store.append(dialogue(
                    new UUID(0L, 8_000L + i),
                    npc,
                    currentPlayer,
                    10_000L + i,
                    "Player said: обычный разговор номер " + i + " о погоде и деревне | NPC replied: Хорошо."
            ), 128);
        }

        assertTrue(
                store.getRecent(npc, 128).stream().anyMatch(event -> event.summary().contains("amber-pine-314")),
                "the installed regression is retrieval, not persistence: the old marker must still exist in Memory 2.0"
        );

        List<String> context = queryAwareLoad(
                tempDir,
                npc,
                currentPlayer,
                20_000L,
                "Муаммер, назови личный маркер, который я ранее сообщил именно тебе."
        );

        assertTrue(context.size() <= 6, "query-aware recall must preserve the existing hard result bound");
        assertTrue(
                context.stream().anyMatch(line -> line.contains("amber-pine-314")),
                "a targeted recall query must rescue the relevant owner-local dialogue before the 32-candidate cap"
        );
        assertTrue(
                context.stream().noneMatch(line -> line.contains("foreign-player-secret-991")),
                "query relevance must never bypass current-player eligibility"
        );
        assertTrue(
                context.stream().noneMatch(line -> line.contains("other-npc-secret-992")),
                "query relevance must never bypass NPC ownership"
        );
    }

    @SuppressWarnings("unchecked")
    private static List<String> queryAwareLoad(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            long gameTime,
            String currentMessage
    ) throws Exception {
        final Method load;
        try {
            load = Memory2ContextProvider.class.getDeclaredMethod(
                    "load",
                    Path.class,
                    UUID.class,
                    UUID.class,
                    long.class,
                    String.class
            );
        } catch (NoSuchMethodException e) {
            fail("RED: Memory2ContextProvider has no query-aware bounded load API yet", e);
            throw new AssertionError("unreachable");
        }

        try {
            return (List<String>) load.invoke(null, worldRoot, npcId, playerId, gameTime, currentMessage);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    private static MemoryEvent dialogue(
            UUID id,
            UUID ownerNpcId,
            UUID playerId,
            long gameTime,
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
                40,
                0,
                60,
                List.of()
        );
    }
}
