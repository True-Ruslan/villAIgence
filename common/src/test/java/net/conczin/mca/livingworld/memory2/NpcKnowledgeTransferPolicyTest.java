package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcKnowledgeTransferPolicyTest {
    private static final UUID SPEAKER = UUID.fromString("00000000-0000-0000-0000-000000021001");
    private static final UUID LISTENER = UUID.fromString("00000000-0000-0000-0000-000000021002");
    private static final UUID SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000021003");
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000021004");
    private static final UUID OTHER_ENTITY = UUID.fromString("00000000-0000-0000-0000-000000021005");
    private static final UUID SOURCE_EVENT_A = UUID.fromString("00000000-0000-0000-0000-000000021006");
    private static final UUID SOURCE_EVENT_B = UUID.fromString("00000000-0000-0000-0000-000000021007");

    @Test
    void validatesRequestAndCanonicalSourceSnapshotFailClosed() {
        SemanticMemoryEntry source = fact(
                SOURCE_ID, SPEAKER, "Bridge   destroyed",
                List.of(PLAYER, OTHER_ENTITY), List.of(SOURCE_EVENT_A, SOURCE_EVENT_B));
        SemanticMemoryEntry equivalent = fact(
                SOURCE_ID, SPEAKER, "Bridge destroyed",
                List.of(OTHER_ENTITY, PLAYER), List.of(SOURCE_EVENT_B, SOURCE_EVENT_A));

        assertTrue(NpcKnowledgeTransferPolicy.validRequest(SPEAKER, LISTENER, source));
        assertFalse(NpcKnowledgeTransferPolicy.validRequest(SPEAKER, SPEAKER, source));
        assertFalse(NpcKnowledgeTransferPolicy.validRequest(null, LISTENER, source));
        assertFalse(NpcKnowledgeTransferPolicy.validRequest(SPEAKER, null, source));
        assertFalse(NpcKnowledgeTransferPolicy.validRequest(SPEAKER, LISTENER, null));
        assertFalse(NpcKnowledgeTransferPolicy.validRequest(
                SPEAKER, LISTENER,
                fact(SOURCE_ID, UUID.randomUUID(), "Bridge destroyed", List.of(PLAYER), List.of(SOURCE_EVENT_A))));

        assertTrue(NpcKnowledgeTransferPolicy.sameSourceSnapshot(source, equivalent));
        assertFalse(NpcKnowledgeTransferPolicy.sameSourceSnapshot(source, null));
        assertFalse(NpcKnowledgeTransferPolicy.sameSourceSnapshot(source, fact(
                UUID.randomUUID(), SPEAKER, "Bridge destroyed",
                List.of(PLAYER, OTHER_ENTITY), List.of(SOURCE_EVENT_A, SOURCE_EVENT_B))));
        assertFalse(NpcKnowledgeTransferPolicy.sameSourceSnapshot(source, fact(
                SOURCE_ID, UUID.randomUUID(), "Bridge destroyed",
                List.of(PLAYER, OTHER_ENTITY), List.of(SOURCE_EVENT_A, SOURCE_EVENT_B))));
        assertFalse(NpcKnowledgeTransferPolicy.sameSourceSnapshot(source, fact(
                SOURCE_ID, SPEAKER, "Bridge repaired",
                List.of(PLAYER, OTHER_ENTITY), List.of(SOURCE_EVENT_A, SOURCE_EVENT_B))));
        assertFalse(NpcKnowledgeTransferPolicy.sameSourceSnapshot(source, fact(
                SOURCE_ID, SPEAKER, "Bridge destroyed",
                List.of(PLAYER), List.of(SOURCE_EVENT_A, SOURCE_EVENT_B))));
        assertFalse(NpcKnowledgeTransferPolicy.sameSourceSnapshot(source, fact(
                SOURCE_ID, SPEAKER, "Bridge destroyed",
                List.of(PLAYER, OTHER_ENTITY), List.of(SOURCE_EVENT_A))));

        SemanticMemoryEntry playerBelief = belief(
                SOURCE_ID, SPEAKER, MemoryEvent.Provenance.PLAYER_TOLD,
                "Bridge destroyed", List.of(PLAYER, OTHER_ENTITY), List.of(SOURCE_EVENT_A, SOURCE_EVENT_B));
        SemanticMemoryEntry npcBelief = belief(
                SOURCE_ID, SPEAKER, MemoryEvent.Provenance.NPC_TOLD,
                "Bridge destroyed", List.of(PLAYER, OTHER_ENTITY), List.of(SOURCE_EVENT_A, SOURCE_EVENT_B));
        assertFalse(NpcKnowledgeTransferPolicy.sameSourceSnapshot(source, playerBelief));
        assertFalse(NpcKnowledgeTransferPolicy.sameSourceSnapshot(playerBelief, npcBelief));
    }

    @Test
    void validatesEveryCanonicalPersistedEvidenceField() {
        String statement = "Bridge destroyed";
        long gameTime = 1234L;
        MemoryEvent canonical = canonicalEvidence(statement, gameTime);

        assertTrue(NpcKnowledgeTransferPolicy.validEvidence(
                canonical, SPEAKER, LISTENER, SOURCE_ID, gameTime, statement));

        assertInvalidEvidence(copy(canonical, UUID.randomUUID(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), SPEAKER, canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                List.of(SPEAKER, LISTENER), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                List.of(LISTENER, SPEAKER, PLAYER), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), MemoryEvent.Type.ACTION, canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), MemoryEvent.Provenance.PLAYER_TOLD, canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), gameTime + 1L, canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), 1L,
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                51, canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), 1, canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), 51, canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), List.of("reason"),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                new MemoryEvent.DialogueExchange("npc speech", "listener reply"), canonical.relationshipTransition(),
                canonical.relationshipCause()), statement, gameTime);

        MemoryEvent.RelationshipTransition transition = new MemoryEvent.RelationshipTransition(
                0, 0, 0, 0, 1, 1, 1, 1);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), transition, canonical.relationshipCause()), statement, gameTime);
        MemoryEvent.RelationshipCause cause = new MemoryEvent.RelationshipCause(
                MemoryEvent.CauseKind.DIALOGUE_TURN, UUID.randomUUID(), UUID.randomUUID(), transition);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), canonical.summary(),
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), cause), statement, gameTime);
        assertInvalidEvidence(copy(canonical, canonical.id(), canonical.ownerNpcId(), canonical.type(), "NPC told: forged",
                canonical.participants(), canonical.provenance(), canonical.gameTime(), canonical.createdAtEpochMillis(),
                canonical.importance(), canonical.emotionalWeight(), canonical.confidence(), canonical.relationshipReasons(),
                canonical.dialogue(), canonical.relationshipTransition(), canonical.relationshipCause()), statement, gameTime);
    }

    @Test
    void retainedBeliefCompatibilityUsesCanonicalScopeAndRequiresExactEvidenceSource() {
        UUID evidenceId = UUID.fromString("00000000-0000-0000-0000-000000021100");
        UUID corroboratingId = UUID.fromString("00000000-0000-0000-0000-000000021101");
        SemanticMemoryEntry expected = belief(
                UUID.randomUUID(), LISTENER, MemoryEvent.Provenance.NPC_TOLD,
                "Bridge destroyed", List.of(PLAYER, OTHER_ENTITY), List.of(evidenceId));
        SemanticMemoryEntry retained = belief(
                UUID.randomUUID(), LISTENER, MemoryEvent.Provenance.NPC_TOLD,
                "bridge   destroyed", List.of(OTHER_ENTITY, PLAYER), List.of(corroboratingId, evidenceId));

        assertTrue(NpcKnowledgeTransferPolicy.compatibleRetainedBelief(expected, retained, evidenceId));
        assertFalse(NpcKnowledgeTransferPolicy.compatibleRetainedBelief(
                expected,
                belief(UUID.randomUUID(), LISTENER, MemoryEvent.Provenance.NPC_TOLD,
                        "Bridge destroyed", List.of(PLAYER, OTHER_ENTITY), List.of(corroboratingId)), evidenceId));
        assertFalse(NpcKnowledgeTransferPolicy.compatibleRetainedBelief(
                expected,
                belief(UUID.randomUUID(), SPEAKER, MemoryEvent.Provenance.NPC_TOLD,
                        "Bridge destroyed", List.of(PLAYER, OTHER_ENTITY), List.of(evidenceId)), evidenceId));
        assertFalse(NpcKnowledgeTransferPolicy.compatibleRetainedBelief(
                expected,
                belief(UUID.randomUUID(), LISTENER, MemoryEvent.Provenance.PLAYER_TOLD,
                        "Bridge destroyed", List.of(PLAYER, OTHER_ENTITY), List.of(evidenceId)), evidenceId));
        assertFalse(NpcKnowledgeTransferPolicy.compatibleRetainedBelief(
                expected,
                belief(UUID.randomUUID(), LISTENER, MemoryEvent.Provenance.NPC_TOLD,
                        "Bridge repaired", List.of(PLAYER, OTHER_ENTITY), List.of(evidenceId)), evidenceId));
        assertFalse(NpcKnowledgeTransferPolicy.compatibleRetainedBelief(
                expected,
                belief(UUID.randomUUID(), LISTENER, MemoryEvent.Provenance.NPC_TOLD,
                        "Bridge destroyed", List.of(PLAYER), List.of(evidenceId)), evidenceId));
    }

    private static MemoryEvent canonicalEvidence(String statement, long gameTime) {
        SemanticMemoryEntry source = fact(
                SOURCE_ID, SPEAKER, statement, List.of(), List.of(SOURCE_EVENT_A));
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                SPEAKER, LISTENER, SOURCE_ID, gameTime);
        KnowledgeTransferProvenance provenance = KnowledgeTransferProvenanceFactory.firstHop(
                source, LISTENER, evidenceId, gameTime).orElseThrow();
        return NpcToldDialogueAdapter.create(
                SPEAKER, LISTENER, SOURCE_ID, gameTime, statement, provenance).orElseThrow();
    }

    private static void assertInvalidEvidence(MemoryEvent event, String statement, long gameTime) {
        assertFalse(NpcKnowledgeTransferPolicy.validEvidence(
                event, SPEAKER, LISTENER, SOURCE_ID, gameTime, statement));
    }

    private static SemanticMemoryEntry fact(
            UUID id, UUID owner, String statement, List<UUID> related, List<UUID> sources) {
        return new SemanticMemoryEntry(
                id, owner, SemanticMemoryEntry.Kind.FACT, statement, related,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 10L, 20L, 70, 100, sources);
    }

    private static SemanticMemoryEntry belief(
            UUID id, UUID owner, MemoryEvent.Provenance provenance,
            String statement, List<UUID> related, List<UUID> sources) {
        return new SemanticMemoryEntry(
                id, owner, SemanticMemoryEntry.Kind.BELIEF, statement, related,
                provenance, 10L, 20L, 50, 50, sources);
    }

    private static MemoryEvent copy(
            MemoryEvent base,
            UUID id,
            UUID owner,
            MemoryEvent.Type type,
            String summary,
            List<UUID> participants,
            MemoryEvent.Provenance provenance,
            long gameTime,
            long createdAt,
            int importance,
            int emotionalWeight,
            int confidence,
            List<String> reasons,
            MemoryEvent.DialogueExchange dialogue,
            MemoryEvent.RelationshipTransition transition,
            MemoryEvent.RelationshipCause cause
    ) {
        return new MemoryEvent(
                id, owner, type, summary, participants, provenance, gameTime, createdAt,
                importance, emotionalWeight, confidence, reasons, dialogue, transition, cause,
                base.knowledgeTransferProvenance());
    }
}
