package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.ai.SemanticBeliefCandidateParser;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Post-dialogue semantic admission for bounded claims explicitly attributed to the current player.
 *
 * <p>The provider supplies statement text only. Ownership, provenance, player identity and source
 * evidence are fixed here from server-owned arguments and the persisted DIALOGUE event.</p>
 */
public final class PlayerToldBeliefLifecycle {
    private PlayerToldBeliefLifecycle() {
    }

    public static void recordCandidatesIfEnabled(
            boolean enabled,
            Path worldRoot,
            MemoryEvent sourceDialogue,
            UUID playerId,
            List<String> candidates,
            int maxCandidates,
            int maxEntriesPerNpc
    ) {
        if (!enabled || worldRoot == null || sourceDialogue == null || playerId == null
                || !sourceDialogue.participants().contains(playerId)
                || candidates == null || candidates.isEmpty()) {
            return;
        }

        int limit = SemanticBeliefCandidateParser.normalizeMaxCandidates(maxCandidates);
        int accepted = 0;
        for (String candidate : candidates) {
            if (accepted >= limit) break;
            if (candidate == null || candidate.isBlank()) continue;

            ControlledSemanticBeliefProducer.recordIfEnabled(
                    true,
                    worldRoot,
                    sourceDialogue,
                    candidate,
                    List.of(playerId),
                    MemoryEvent.Provenance.PLAYER_TOLD,
                    sourceDialogue.importance(),
                    sourceDialogue.confidence(),
                    maxEntriesPerNpc
            );
            accepted++;
        }
    }
}
