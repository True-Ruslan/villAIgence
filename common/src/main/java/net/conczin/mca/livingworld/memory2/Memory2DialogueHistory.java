package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Reconstructs bounded recent dialogue for one exact NPC/player pair from Memory 2.0. */
public final class Memory2DialogueHistory {
    private static final int MAX_EXCHANGES = WorkingMemoryOrchestrator.MAX_RECENT_DIALOGUE_MESSAGES / 2;
    private static final Set<String> QUERY_STOP_WORDS = Set.of(
            "about", "are", "did", "does", "for", "from", "have", "how", "is", "me", "my", "please",
            "remember", "tell", "that", "the", "this", "was", "were", "what", "when", "where", "which", "who",
            "you", "your",
            "был", "была", "были", "есть", "как", "какая", "какие", "какой", "какое", "мне", "мой", "мои",
            "моя", "моё", "мое", "помнишь", "про", "скажи", "тебе", "тебя", "ты", "что", "это"
    );
    private static final Comparator<MemoryEvent> OLDEST_FIRST = Comparator
            .comparingLong(MemoryEvent::gameTime)
            .thenComparingLong(MemoryEvent::createdAtEpochMillis)
            .thenComparing(event -> event.id().toString());

    private Memory2DialogueHistory() {
    }

    public static List<WorkingMemoryMessage> load(Path worldRoot, UUID npcId, UUID playerId) {
        return load(worldRoot, npcId, playerId, null);
    }

    public static List<WorkingMemoryMessage> load(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            String currentPlayerMessage
    ) {
        if (worldRoot == null || npcId == null || playerId == null) return List.of();

        List<MemoryEvent> newestFirst = MemoryEventStore.forWorld(worldRoot).getRecentMatching(
                npcId,
                Integer.MAX_VALUE,
                event -> isDialogueFor(event, npcId, playerId)
        );
        if (newestFirst.isEmpty()) return List.of();

        List<MemoryEvent> selected = selectBounded(newestFirst, currentPlayerMessage);
        selected.sort(OLDEST_FIRST);

        List<WorkingMemoryMessage> messages = new ArrayList<>(selected.size() * 2);
        for (MemoryEvent event : selected) {
            MemoryEvent.DialogueExchange dialogue = event.dialogue();
            messages.add(new WorkingMemoryMessage("user", dialogue.playerMessage()));
            messages.add(new WorkingMemoryMessage("assistant", dialogue.npcReply()));
        }

        return WorkingMemoryOrchestrator.compose(messages, List.of(), List.of()).recentDialogue();
    }

    private static List<MemoryEvent> selectBounded(List<MemoryEvent> newestFirst, String currentPlayerMessage) {
        int recentCount = Math.min(MAX_EXCHANGES, newestFirst.size());
        List<MemoryEvent> selected = new ArrayList<>(newestFirst.subList(0, recentCount));
        if (selected.size() < MAX_EXCHANGES || newestFirst.size() <= MAX_EXCHANGES) return selected;

        Set<String> queryTokens = meaningfulTokens(currentPlayerMessage);
        if (queryTokens.isEmpty()) return selected;

        int bestRecentScore = selected.stream()
                .mapToInt(event -> relevanceScore(event, queryTokens))
                .max()
                .orElse(0);

        MemoryEvent bestOlder = null;
        int bestOlderScore = 0;
        for (int index = MAX_EXCHANGES; index < newestFirst.size(); index++) {
            MemoryEvent candidate = newestFirst.get(index);
            int score = relevanceScore(candidate, queryTokens);
            if (score > bestOlderScore) {
                bestOlder = candidate;
                bestOlderScore = score;
            }
        }

        if (bestOlder != null && bestOlderScore > bestRecentScore) {
            selected.remove(selected.size() - 1);
            selected.add(bestOlder);
        }
        return selected;
    }

    private static int relevanceScore(MemoryEvent event, Set<String> queryTokens) {
        MemoryEvent.DialogueExchange dialogue = event.dialogue();
        if (dialogue == null || queryTokens.isEmpty()) return 0;

        Set<String> dialogueTokens = meaningfulTokens(dialogue.playerMessage() + " " + dialogue.npcReply());
        int score = 0;
        for (String token : queryTokens) {
            if (dialogueTokens.contains(token)) score++;
        }
        return score;
    }

    private static Set<String> meaningfulTokens(String text) {
        if (text == null || text.isBlank()) return Set.of();

        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (token.length() >= 3 && !QUERY_STOP_WORDS.contains(token)) tokens.add(token);
        }
        return Set.copyOf(tokens);
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
