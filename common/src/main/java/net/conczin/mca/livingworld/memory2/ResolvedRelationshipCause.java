package net.conczin.mca.livingworld.memory2;

import java.util.Optional;
import java.util.UUID;

/** Read-side view of one causal relationship event and any source events still retained in Memory 2.0. */
public record ResolvedRelationshipCause(
        MemoryEvent causeEvent,
        MemoryEvent.RelationshipTransition transition,
        UUID relationshipChangeEventId,
        Optional<MemoryEvent> relationshipChangeEvent,
        UUID evidenceEventId,
        Optional<MemoryEvent> evidenceEvent
) {
    public ResolvedRelationshipCause {
        if (causeEvent == null) throw new IllegalArgumentException("causeEvent is required");
        if (transition == null) throw new IllegalArgumentException("transition is required");
        if (relationshipChangeEventId == null) throw new IllegalArgumentException("relationshipChangeEventId is required");
        if (evidenceEventId == null) throw new IllegalArgumentException("evidenceEventId is required");
        relationshipChangeEvent = relationshipChangeEvent == null ? Optional.empty() : relationshipChangeEvent;
        evidenceEvent = evidenceEvent == null ? Optional.empty() : evidenceEvent;
    }
}
