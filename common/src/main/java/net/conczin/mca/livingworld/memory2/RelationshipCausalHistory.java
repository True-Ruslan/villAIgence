package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Exact NPC/player read surface for deterministic causal relationship history. */
public final class RelationshipCausalHistory {
    private RelationshipCausalHistory() {
    }

    public static List<ResolvedRelationshipCause> getRecent(
            Path worldRoot,
            UUID npcId,
            UUID playerId,
            int maxResults
    ) {
        if (worldRoot == null || npcId == null || playerId == null || maxResults <= 0) return List.of();

        MemoryEventStore store = MemoryEventStore.forWorld(worldRoot);
        return store.getRecentMatching(
                        npcId,
                        maxResults,
                        event -> isEligibleCause(event, npcId, playerId)
                ).stream()
                .map(cause -> resolve(store, cause, npcId, playerId))
                .toList();
    }

    private static boolean isEligibleCause(MemoryEvent event, UUID npcId, UUID playerId) {
        if (event == null || event.type() != MemoryEvent.Type.RELATIONSHIP_CAUSE) return false;
        if (!npcId.equals(event.ownerNpcId())) return false;
        if (!event.participants().contains(npcId) || !event.participants().contains(playerId)) return false;
        MemoryEvent.RelationshipCause cause = event.relationshipCause();
        return cause != null
                && cause.kind() == MemoryEvent.CauseKind.DIALOGUE_TURN
                && cause.relationshipChangeEventId() != null
                && cause.evidenceEventId() != null
                && cause.transitionSnapshot() != null;
    }

    private static ResolvedRelationshipCause resolve(
            MemoryEventStore store,
            MemoryEvent causeEvent,
            UUID npcId,
            UUID playerId
    ) {
        MemoryEvent.RelationshipCause cause = causeEvent.relationshipCause();
        Optional<MemoryEvent> relationshipChange = resolveSource(
                store,
                npcId,
                playerId,
                cause.relationshipChangeEventId(),
                MemoryEvent.Type.RELATIONSHIP_CHANGE
        ).filter(event -> cause.transitionSnapshot().equals(event.relationshipTransition()));
        Optional<MemoryEvent> evidence = resolveSource(
                store,
                npcId,
                playerId,
                cause.evidenceEventId(),
                MemoryEvent.Type.DIALOGUE
        );

        return new ResolvedRelationshipCause(
                causeEvent,
                cause.transitionSnapshot(),
                cause.relationshipChangeEventId(),
                relationshipChange,
                cause.evidenceEventId(),
                evidence
        );
    }

    private static Optional<MemoryEvent> resolveSource(
            MemoryEventStore store,
            UUID npcId,
            UUID playerId,
            UUID sourceId,
            MemoryEvent.Type expectedType
    ) {
        return store.getRecentMatching(
                        npcId,
                        Integer.MAX_VALUE,
                        event -> sourceId.equals(event.id())
                                && event.type() == expectedType
                                && npcId.equals(event.ownerNpcId())
                                && event.participants().contains(npcId)
                                && event.participants().contains(playerId)
                ).stream()
                .findFirst();
    }
}
