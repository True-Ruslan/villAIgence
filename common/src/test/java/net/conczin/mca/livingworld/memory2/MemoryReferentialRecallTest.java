package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryReferentialRecallTest {
    private static final UUID NPC = UUID.fromString("34e2a220-7e85-4edc-8c93-52b068b97608");
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000702");

    @Test
    void russianMarkerRecallBoostsCompactOpaquePlayerToldMarker() {
        String query = "Муаммер, назови личный маркер, который я ранее сообщил именно тебе.";
        MemoryEvent event = dialogue(
                MemoryEvent.Provenance.PLAYER_TOLD,
                "Player said: Запомни amber-pine-314. | NPC replied: Хорошо.",
                null
        );

        assertTrue(MemoryReferentialRecall.hasRecallIntent(query));
        assertTrue(MemoryReferentialRecall.containsOpaqueMarker(event.summary()));
        assertEquals(100, MemoryReferentialRecall.score(query, event));
    }

    @Test
    void englishMarkerRecallSupportsSpokenOpaqueMarkerForm() {
        String query = "What marker did I tell you earlier?";
        MemoryEvent event = dialogue(
                MemoryEvent.Provenance.PLAYER_TOLD,
                "dialogue summary without the marker value",
                new MemoryEvent.DialogueExchange("Please remember amber pine 314", "Okay")
        );

        assertTrue(MemoryReferentialRecall.hasRecallIntent(query));
        assertEquals(100, MemoryReferentialRecall.score(query, event));
    }

    @Test
    void storageInstructionDoesNotMasqueradeAsRecallIntent() {
        String query = "Запомни мой маркер amber-pine-314.";

        assertFalse(MemoryReferentialRecall.hasRecallIntent(query));
        assertEquals(0, MemoryReferentialRecall.score(
                query,
                dialogue(MemoryEvent.Provenance.PLAYER_TOLD,
                        "Player said: amber-pine-314", null)
        ));
    }

    @Test
    void npcToldDialogueDoesNotGainPlayerMarkerRecallBoost() {
        String query = "Муаммер, назови маркер, который я ранее сообщил тебе.";

        assertEquals(0, MemoryReferentialRecall.score(
                query,
                dialogue(MemoryEvent.Provenance.NPC_TOLD,
                        "NPC said: amber-pine-314", null)
        ));
    }

    @Test
    void ordinaryNumberDoesNotBecomeOpaqueMarker() {
        String query = "Муаммер, назови маркер, который я ранее сообщил тебе.";
        MemoryEvent event = dialogue(
                MemoryEvent.Provenance.PLAYER_TOLD,
                "Player said: в деревне живут 314 жителей. | NPC replied: Понятно.",
                null
        );

        assertFalse(MemoryReferentialRecall.containsOpaqueMarker(event.summary()));
        assertEquals(0, MemoryReferentialRecall.score(query, event));
    }

    @Test
    void unrelatedTechnicalIdentifierNeedsAnExplicitStorageCue() {
        String query = "Муаммер, назови маркер, который я ранее сообщил тебе.";
        MemoryEvent event = dialogue(
                MemoryEvent.Provenance.PLAYER_TOLD,
                "Player said: На сервере используется java-21. | NPC replied: Понятно.",
                null
        );

        assertTrue(MemoryReferentialRecall.containsOpaqueMarker(event.summary()));
        assertEquals(
                0,
                MemoryReferentialRecall.score(query, event),
                "an arbitrary machine-like identifier must not become a personal marker without a storage cue"
        );
    }

    private static MemoryEvent dialogue(
            MemoryEvent.Provenance provenance,
            String summary,
            MemoryEvent.DialogueExchange exchange
    ) {
        return new MemoryEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000711"),
                NPC,
                MemoryEvent.Type.DIALOGUE,
                summary,
                List.of(NPC, PLAYER),
                provenance,
                1L,
                1_700_000_000_001L,
                40,
                0,
                60,
                List.of(),
                exchange
        );
    }
}
