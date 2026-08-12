package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Memory2DialogueHistoryTest {
    @TempDir
    Path tempDir;

    @Test
    void storesStructuredDialogueWithoutDependingOnSummaryParsing() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        String playerText = "I literally typed | NPC replied: not a delimiter";
        String npcText = "And I can say Player said: without changing roles";

        MemoryEvent event = DialogueMemoryAdapter.toMemoryEvent(
                npc, player, 42L, playerText, npcText, 1_000L
        ).orElseThrow();

        MemoryEvent.DialogueExchange dialogue = event.dialogue();
        assertNotNull(dialogue);
        assertEquals(playerText, dialogue.playerMessage());
        assertEquals(npcText, dialogue.npcReply());

        Memory2DialogueIngestor.record(tempDir, npc, player, 42L, playerText, npcText, 64, 1_000L);
        List<WorkingMemoryMessage> history = Memory2DialogueHistory.load(tempDir, npc, player);

        assertEquals(List.of(
                new WorkingMemoryMessage("user", playerText),
                new WorkingMemoryMessage("assistant", npcText)
        ), history);
    }

    @Test
    void filtersBeforeLimitingAndPreservesExactNpcPlayerIsolation() {
        UUID npc = UUID.randomUUID();
        UUID otherNpc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();

        for (int turn = 0; turn < 7; turn++) {
            Memory2DialogueIngestor.record(
                    tempDir,
                    npc,
                    player,
                    100L + turn,
                    "target user " + turn,
                    "target npc " + turn,
                    128,
                    1_000L + turn
            );
        }

        Memory2DialogueIngestor.record(
                tempDir, npc, otherPlayer, 200L, "other player", "must not leak", 128, 2_000L
        );
        Memory2DialogueIngestor.record(
                tempDir, otherNpc, player, 201L, "other npc", "must not leak", 128, 2_001L
        );

        for (int index = 0; index < 20; index++) {
            MemoryEventStore.forWorld(tempDir).append(new MemoryEvent(
                    UUID.nameUUIDFromBytes(("action-" + index).getBytes()),
                    npc,
                    MemoryEvent.Type.ACTION,
                    "newer non-dialogue event " + index,
                    List.of(npc),
                    MemoryEvent.Provenance.SYSTEM_OBSERVED,
                    300L + index,
                    3_000L + index,
                    60,
                    0,
                    100,
                    List.of()
            ), 128);
        }

        List<WorkingMemoryMessage> history = Memory2DialogueHistory.load(tempDir, npc, player);

        assertEquals(WorkingMemoryOrchestrator.MAX_RECENT_DIALOGUE_MESSAGES, history.size());
        for (int exchange = 0; exchange < 6; exchange++) {
            int expectedTurn = exchange + 1;
            assertEquals("user", history.get(exchange * 2).role());
            assertEquals("target user " + expectedTurn, history.get(exchange * 2).content());
            assertEquals("assistant", history.get(exchange * 2 + 1).role());
            assertEquals("target npc " + expectedTurn, history.get(exchange * 2 + 1).content());
        }
    }

    @Test
    void queryCanRecoverRelevantOlderExchangeWithoutCrossPlayerLeakOrGrowingPrompt() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();

        Memory2DialogueIngestor.record(
                tempDir,
                npc,
                player,
                10L,
                "My calibration phrase is amber-orchid-731",
                "I will remember that calibration phrase.",
                128,
                1_000L
        );
        Memory2DialogueIngestor.record(
                tempDir,
                npc,
                otherPlayer,
                11L,
                "My calibration phrase is private-cobalt-999",
                "That phrase belongs only to you.",
                128,
                1_001L
        );

        for (int turn = 0; turn < 7; turn++) {
            Memory2DialogueIngestor.record(
                    tempDir,
                    npc,
                    player,
                    20L + turn,
                    "Unrelated recent topic " + turn + " about weather and chores",
                    "Unrelated recent reply " + turn,
                    128,
                    2_000L + turn
            );
        }

        List<WorkingMemoryMessage> history = Memory2DialogueHistory.load(
                tempDir,
                npc,
                player,
                "What is my calibration phrase?"
        );

        assertEquals(WorkingMemoryOrchestrator.MAX_RECENT_DIALOGUE_MESSAGES, history.size());
        assertEquals(true, history.stream().anyMatch(message -> message.content().contains("amber-orchid-731")));
        assertEquals(false, history.stream().anyMatch(message -> message.content().contains("private-cobalt-999")));
    }

    @Test
    void ignoresLegacyDialogueEventsWithoutStructuredPayload() {
        UUID npc = UUID.randomUUID();
        UUID player = UUID.randomUUID();

        MemoryEventStore.forWorld(tempDir).append(new MemoryEvent(
                UUID.randomUUID(),
                npc,
                MemoryEvent.Type.DIALOGUE,
                "Player said: historical | NPC replied: summary-only",
                List.of(npc, player),
                MemoryEvent.Provenance.PLAYER_TOLD,
                10L,
                10L,
                40,
                0,
                60,
                List.of()
        ), 64);

        assertEquals(List.of(), Memory2DialogueHistory.load(tempDir, npc, player));
    }
}
