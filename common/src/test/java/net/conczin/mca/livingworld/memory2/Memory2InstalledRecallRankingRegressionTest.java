package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Memory2InstalledRecallRankingRegressionTest {
    @TempDir
    Path tempDir;

    @Test
    void oneTokenTargetedRecallSurvivesFinalRankToSixAfterInstalledHistoryAgesOut() {
        UUID npc = UUID.fromString("34e2a220-7e85-4edc-8c93-52b068b97608");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000702");
        UUID markerEventId = UUID.fromString("3252f67f-27f5-38bf-840c-d522c36b34fd");
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

        String query = "Муаммер, назови личный маркер, который я ранее сообщил именно тебе.";
        MemoryEvent oldMarker = dialogue(
                markerEventId,
                npc,
                player,
                1L,
                "Player said: Запомни этот маркер amber-pine-314. | NPC replied: Хорошо."
        );
        store.append(oldMarker, 128);

        appendFreshNoise(store, npc, player);

        assertEquals(
                50,
                MemoryLexicalRelevance.score(query, oldMarker.summary()),
                "the installed-shaped regression deliberately has only one useful lexical overlap"
        );
        assertEquals(
                14,
                store.getRecent(npc, 128).size(),
                "the failure is not retention: all installed-shaped dialogue events must remain stored"
        );
        assertTrue(
                store.getRecent(npc, 128).stream().anyMatch(event -> event.id().equals(markerEventId)),
                "the preserved marker event must still exist before retrieval"
        );

        List<String> context = Memory2ContextProvider.load(
                tempDir,
                npc,
                player,
                300_000L,
                query
        );

        assertTrue(context.size() <= 6, "the existing final prompt result bound must remain hard");
        assertTrue(
                context.stream().anyMatch(line -> line.contains("amber-pine-314")),
                "one-token targeted relevance must outrank unrelated fresh dialogue at the final rank-to-6 boundary"
        );
    }

    @Test
    void markerRecallIntentRecoversOpaquePlayerToldMarkerWithoutLexicalOverlap() {
        UUID npc = UUID.fromString("34e2a220-7e85-4edc-8c93-52b068b97608");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000702");
        UUID markerEventId = UUID.fromString("3252f67f-27f5-38bf-840c-d522c36b34fd");
        MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

        String query = "Муаммер, назови личный маркер, который я ранее сообщил именно тебе.";
        MemoryEvent oldMarker = dialogue(
                markerEventId,
                npc,
                player,
                1L,
                "Player said: Запомни amber-pine-314. | NPC replied: Хорошо."
        );
        store.append(oldMarker, 128);

        appendFreshNoise(store, npc, player);

        assertEquals(
                0,
                MemoryLexicalRelevance.score(query, oldMarker.summary()),
                "the hardened regression must not depend on any shared useful lexical token"
        );
        assertEquals(14, store.getRecent(npc, 128).size());
        assertTrue(
                store.getRecent(npc, 128).stream().anyMatch(event -> event.id().equals(markerEventId)),
                "the source marker remains retained; this is retrieval, not persistence loss"
        );

        List<String> context = Memory2ContextProvider.load(
                tempDir,
                npc,
                player,
                300_000L,
                query
        );

        assertTrue(context.size() <= 6, "referential recall must not widen the final result bound");
        assertTrue(
                context.stream().anyMatch(line -> line.contains("amber-pine-314")),
                "RED: explicit marker-recall intent must recover an older opaque player-told marker even with zero lexical overlap"
        );
    }

    private static void appendFreshNoise(MemoryEventStore store, UUID npc, UUID player) {
        for (int i = 0; i < 13; i++) {
            store.append(dialogue(
                    new UUID(0L, 9_000L + i),
                    npc,
                    player,
                    290_000L + i,
                    "Player said: обычный разговор номер " + i
                            + " о погоде и деревне | NPC replied: Хорошо."
            ), 128);
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
