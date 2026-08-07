package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Reconstructs bounded recent dialogue for one exact NPC/player pair from Memory 2.0. */
public final class Memory2DialogueHistory {
    private static final int MAX_EXCHANGES = WorkingMemoryOrchestrator.MAX_RECENT_DIALOGUE_MESSAGES / 2;

    private Memory2DialogueHistory() {
    }

    public static List<WorkingMemoryMessage> load(Path worldRoot, UUID npcId, UUID playerId) {
        if (worldRoot == null || npcId == null || playerId == null) return List.of();

        List<MemoryEvent> newestFirst = MemoryEventStore.forWorld(worldRoot).getRecentMatching(
                npcId,
                MAX_EXCHANGES,
                event -> isDialogueFor(event, npcId, playerId)
        );
        if (newestFirst.isEmpty()) return List.of();

        List<MemoryEvent> chronological = new ArrayList<>(newestFirst);
        Collections.reverse(chronological);
        List<WorkingMemoryMessage> messages = new ArrayList<>(chronological.size() * 2);
        for (MemoryEvent event : chronological) {
            MemoryEvent.DialogueExchange dialogue = event.dialogue();
            messages.add(new WorkingMemoryMessage("user", dialogue.playerMessage()));
            messages.add(new WorkingMemoryMessage("assistant", dialogue.npcReply()));
        }

        return WorkingMemoryOrchestrator.compose(messages, List.of(), List.of()).recentDialogue();
    }

    private static boolean isDialogueFor(MemoryEvent event, UUID npcId, UUID playerId) {
        if (event == null
                || event.type() != MemoryEvent.Type.DIALOGUE
                || event.dialogue() == null
                || !npcId.equals(event.ownerNpcId())) {
            return false;
        }
        List<UUID> participants = event.participants();
        return participants.size() == 2
                && participants.contains(npcId)
                && participants.contains(playerId);
    }
}
