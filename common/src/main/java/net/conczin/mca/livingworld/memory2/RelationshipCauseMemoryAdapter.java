package net.conczin.mca.livingworld.memory2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Creates deterministic server-authored causal links from validated Memory 2.0 source events. */
public final class RelationshipCauseMemoryAdapter {
    private static final String ID_NAMESPACE = "memory2-relationship-cause-v1";
    private static final String DIALOGUE_SUMMARY = "Relationship change occurred during dialogue with player.";

    private RelationshipCauseMemoryAdapter() {
    }

    public static Optional<MemoryEvent> toDialogueTurnCause(
            MemoryEvent relationshipChange,
            MemoryEvent dialogue,
            UUID playerId
    ) {
        if (relationshipChange == null || dialogue == null || playerId == null) return Optional.empty();
        if (relationshipChange.type() != MemoryEvent.Type.RELATIONSHIP_CHANGE) return Optional.empty();
        if (relationshipChange.provenance() != MemoryEvent.Provenance.SYSTEM_OBSERVED) return Optional.empty();
        if (relationshipChange.relationshipTransition() == null) return Optional.empty();
        if (dialogue.type() != MemoryEvent.Type.DIALOGUE) return Optional.empty();
        if (relationshipChange.id().equals(dialogue.id())) return Optional.empty();

        UUID npcId = relationshipChange.ownerNpcId();
        if (!npcId.equals(dialogue.ownerNpcId())) return Optional.empty();
        if (!hasParticipants(relationshipChange, npcId, playerId)) return Optional.empty();
        if (!hasParticipants(dialogue, npcId, playerId)) return Optional.empty();

        MemoryEvent.RelationshipCause cause = new MemoryEvent.RelationshipCause(
                MemoryEvent.CauseKind.DIALOGUE_TURN,
                relationshipChange.id(),
                dialogue.id(),
                relationshipChange.relationshipTransition()
        );
        UUID id = deterministicId(npcId, cause);

        return Optional.of(new MemoryEvent(
                id,
                npcId,
                MemoryEvent.Type.RELATIONSHIP_CAUSE,
                DIALOGUE_SUMMARY,
                List.of(npcId, playerId),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                Math.max(relationshipChange.gameTime(), dialogue.gameTime()),
                Math.max(relationshipChange.createdAtEpochMillis(), dialogue.createdAtEpochMillis()),
                55,
                0,
                100,
                List.of(),
                null,
                null,
                cause
        ));
    }

    private static boolean hasParticipants(MemoryEvent event, UUID npcId, UUID playerId) {
        return event.participants().contains(npcId) && event.participants().contains(playerId);
    }

    private static UUID deterministicId(UUID npcId, MemoryEvent.RelationshipCause cause) {
        String canonical = ID_NAMESPACE
                + '\n' + npcId
                + '\n' + cause.relationshipChangeEventId()
                + '\n' + cause.evidenceEventId()
                + '\n' + cause.kind().name();
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }
}
