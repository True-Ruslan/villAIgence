package net.conczin.mca.livingworld.memory2;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Immutable provider-independent Memory 2.0 event owned by one NPC. */
public record MemoryEvent(
        UUID id,
        UUID ownerNpcId,
        Type type,
        String summary,
        List<UUID> participants,
        Provenance provenance,
        long gameTime,
        long createdAtEpochMillis,
        int importance,
        int emotionalWeight,
        int confidence,
        List<String> relationshipReasons,
        DialogueExchange dialogue,
        RelationshipTransition relationshipTransition,
        RelationshipCause relationshipCause
) {
    public MemoryEvent {
        if (id == null) throw new IllegalArgumentException("id is required");
        if (ownerNpcId == null) throw new IllegalArgumentException("ownerNpcId is required");
        if (type == null) throw new IllegalArgumentException("type is required");
        if (summary == null || summary.isBlank()) throw new IllegalArgumentException("summary is required");
        if (provenance == null) throw new IllegalArgumentException("provenance is required");

        summary = summary.strip();
        participants = normalizeParticipants(participants);
        gameTime = Math.max(0L, gameTime);
        createdAtEpochMillis = Math.max(0L, createdAtEpochMillis);
        importance = clamp(importance, 0, 100);
        emotionalWeight = clamp(emotionalWeight, -100, 100);
        confidence = clamp(confidence, 0, 100);
        relationshipReasons = normalizeReasons(relationshipReasons);
    }

    /**
     * Source-compatible constructor for callers that already provide structured relationship transition data.
     * Structured causal data is absent unless explicitly supplied.
     */
    public MemoryEvent(
            UUID id,
            UUID ownerNpcId,
            Type type,
            String summary,
            List<UUID> participants,
            Provenance provenance,
            long gameTime,
            long createdAtEpochMillis,
            int importance,
            int emotionalWeight,
            int confidence,
            List<String> relationshipReasons,
            DialogueExchange dialogue,
            RelationshipTransition relationshipTransition
    ) {
        this(
                id,
                ownerNpcId,
                type,
                summary,
                participants,
                provenance,
                gameTime,
                createdAtEpochMillis,
                importance,
                emotionalWeight,
                confidence,
                relationshipReasons,
                dialogue,
                relationshipTransition,
                null
        );
    }

    /**
     * Source-compatible constructor for callers that already provide structured dialogue.
     * Structured relationship transition and causal data are absent unless explicitly supplied.
     */
    public MemoryEvent(
            UUID id,
            UUID ownerNpcId,
            Type type,
            String summary,
            List<UUID> participants,
            Provenance provenance,
            long gameTime,
            long createdAtEpochMillis,
            int importance,
            int emotionalWeight,
            int confidence,
            List<String> relationshipReasons,
            DialogueExchange dialogue
    ) {
        this(
                id,
                ownerNpcId,
                type,
                summary,
                participants,
                provenance,
                gameTime,
                createdAtEpochMillis,
                importance,
                emotionalWeight,
                confidence,
                relationshipReasons,
                dialogue,
                null,
                null
        );
    }

    /**
     * Source-compatible constructor for non-dialogue producers and historical tests.
     * Structured dialogue, relationship transition and causal data are opt-in and absent by default.
     */
    public MemoryEvent(
            UUID id,
            UUID ownerNpcId,
            Type type,
            String summary,
            List<UUID> participants,
            Provenance provenance,
            long gameTime,
            long createdAtEpochMillis,
            int importance,
            int emotionalWeight,
            int confidence,
            List<String> relationshipReasons
    ) {
        this(
                id,
                ownerNpcId,
                type,
                summary,
                participants,
                provenance,
                gameTime,
                createdAtEpochMillis,
                importance,
                emotionalWeight,
                confidence,
                relationshipReasons,
                null,
                null,
                null
        );
    }

    private static List<UUID> normalizeParticipants(List<UUID> participants) {
        if (participants == null || participants.isEmpty()) return List.of();
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID participant : participants) {
            if (participant != null) unique.add(participant);
        }
        return List.copyOf(unique);
    }

    private static List<String> normalizeReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) return List.of();
        Set<String> unique = new LinkedHashSet<>();
        for (String reason : reasons) {
            if (reason == null) continue;
            String normalized = reason.strip();
            if (!normalized.isBlank()) unique.add(normalized);
        }
        return List.copyOf(new ArrayList<>(unique));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Structured dialogue payload used to reconstruct prompt history without parsing summaries. */
    public record DialogueExchange(String playerMessage, String npcReply) {
        public DialogueExchange {
            if (playerMessage == null || playerMessage.isBlank()) {
                throw new IllegalArgumentException("playerMessage is required");
            }
            if (npcReply == null || npcReply.isBlank()) {
                throw new IllegalArgumentException("npcReply is required");
            }
            playerMessage = playerMessage.strip();
            npcReply = npcReply.strip();
        }
    }

    /** Exact bounded server-applied relationship state before and after one transition. */
    public record RelationshipTransition(
            int beforeTrust,
            int beforeRespect,
            int beforeFear,
            int beforeAffinity,
            int afterTrust,
            int afterRespect,
            int afterFear,
            int afterAffinity
    ) {
        public RelationshipTransition {
            beforeTrust = clamp(beforeTrust, -100, 100);
            beforeRespect = clamp(beforeRespect, -100, 100);
            beforeFear = clamp(beforeFear, -100, 100);
            beforeAffinity = clamp(beforeAffinity, -100, 100);
            afterTrust = clamp(afterTrust, -100, 100);
            afterRespect = clamp(afterRespect, -100, 100);
            afterFear = clamp(afterFear, -100, 100);
            afterAffinity = clamp(afterAffinity, -100, 100);
        }
    }

    /** Exact server-owned linkage from one relationship transition to its persisted evidence event. */
    public record RelationshipCause(
            CauseKind kind,
            UUID relationshipChangeEventId,
            UUID evidenceEventId,
            RelationshipTransition transitionSnapshot
    ) {
        public RelationshipCause {
            if (kind == null) throw new IllegalArgumentException("cause kind is required");
            if (relationshipChangeEventId == null) throw new IllegalArgumentException("relationshipChangeEventId is required");
            if (evidenceEventId == null) throw new IllegalArgumentException("evidenceEventId is required");
            if (transitionSnapshot == null) throw new IllegalArgumentException("transitionSnapshot is required");
        }
    }

    public enum CauseKind {
        DIALOGUE_TURN
    }

    public enum Type {
        DIALOGUE,
        OBSERVATION,
        ACTION,
        RELATIONSHIP_CHANGE,
        RELATIONSHIP_CAUSE
    }

    public enum Provenance {
        SYSTEM_OBSERVED,
        PLAYER_TOLD,
        NPC_TOLD,
        INFERRED
    }
}
