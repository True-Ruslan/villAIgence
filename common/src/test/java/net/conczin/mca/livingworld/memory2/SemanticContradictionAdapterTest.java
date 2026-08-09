package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticContradictionAdapterTest {
    private static final UUID NPC = id(1);
    private static final UUID PLAYER = id(90);

    @Test
    void pairOrderDoesNotChangeCanonicalEvidence() {
        SemanticMemoryEntry first = fact(id(10), "The gate is open", List.of(PLAYER));
        SemanticMemoryEntry second = belief(id(20), "The gate is closed", List.of(PLAYER));

        MemoryEvent forward = SemanticContradictionAdapter.create(first, second, 100L).orElseThrow();
        MemoryEvent reverse = SemanticContradictionAdapter.create(second, first, 100L).orElseThrow();

        assertEquals(forward, reverse);
        assertEquals(MemoryEvent.Type.SEMANTIC_CONTRADICTION, forward.type());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, forward.provenance());
        assertEquals(List.of(NPC), forward.participants());
        assertEquals("Semantic contradiction recorded", forward.summary());
        assertEquals(0L, forward.createdAtEpochMillis());
        assertEquals(60, forward.importance());
        assertEquals(0, forward.emotionalWeight());
        assertEquals(100, forward.confidence());
        assertTrue(forward.relationshipReasons().isEmpty());
        assertEquals(SemanticMemoryEntry.Kind.FACT, snapshotFor(forward, first).kind());
        assertEquals(MemoryEvent.Provenance.SYSTEM_OBSERVED, snapshotFor(forward, first).provenance());
        assertEquals(SemanticMemoryEntry.Kind.BELIEF, snapshotFor(forward, second).kind());
        assertEquals(MemoryEvent.Provenance.NPC_TOLD, snapshotFor(forward, second).provenance());
    }

    @Test
    void rejectsSameLogicalClaimOwnerMismatchAndScopeMismatch() {
        SemanticMemoryEntry first = belief(id(30), "Bridge blocked", List.of(PLAYER));
        SemanticMemoryEntry same = new SemanticMemoryEntry(
                id(31), NPC, SemanticMemoryEntry.Kind.BELIEF, " bridge   BLOCKED ", List.of(PLAYER),
                MemoryEvent.Provenance.NPC_TOLD, 20L, 0L, 60, 60, List.of(id(131)));
        SemanticMemoryEntry otherOwner = new SemanticMemoryEntry(
                id(32), id(2), SemanticMemoryEntry.Kind.BELIEF, "Bridge open", List.of(PLAYER),
                MemoryEvent.Provenance.NPC_TOLD, 20L, 0L, 60, 60, List.of(id(132)));
        SemanticMemoryEntry otherScope = belief(id(33), "Bridge open", List.of(id(91)));

        assertTrue(SemanticContradictionAdapter.create(first, same, 100L).isEmpty());
        assertTrue(SemanticContradictionAdapter.create(first, otherOwner, 100L).isEmpty());
        assertTrue(SemanticContradictionAdapter.create(first, otherScope, 100L).isEmpty());
        assertTrue(SemanticContradictionAdapter.create(first, first, 100L).isEmpty());
    }

    private static SemanticContradiction.ClaimSnapshot snapshotFor(MemoryEvent event, SemanticMemoryEntry entry) {
        UUID logical = SemanticMemoryIdentity.logicalClaimId(entry);
        SemanticContradiction payload = event.semanticContradiction();
        return payload.first().logicalClaimId().equals(logical) ? payload.first() : payload.second();
    }

    private static SemanticMemoryEntry fact(UUID entryId, String statement, List<UUID> scope) {
        return new SemanticMemoryEntry(
                entryId, NPC, SemanticMemoryEntry.Kind.FACT, statement, scope,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 10L, 0L, 90, 100, List.of(id(900)));
    }

    private static SemanticMemoryEntry belief(UUID entryId, String statement, List<UUID> scope) {
        return new SemanticMemoryEntry(
                entryId, NPC, SemanticMemoryEntry.Kind.BELIEF, statement, scope,
                MemoryEvent.Provenance.NPC_TOLD, 10L, 0L, 50, 50, List.of(id(901)));
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
