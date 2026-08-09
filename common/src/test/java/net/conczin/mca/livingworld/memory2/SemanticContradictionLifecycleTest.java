package net.conczin.mca.livingworld.memory2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticContradictionLifecycleTest {
    @TempDir
    Path tempDir;

    @Test
    void recordsExactCanonicalEvidenceFromPersistedSemanticIdsOnly() {
        Path world = tempDir.resolve("recorded");
        UUID npc = id(1);
        SemanticMemoryEntry first = fact(id(101), npc, "The gate is open", List.of(id(90)));
        SemanticMemoryEntry second = belief(id(102), npc, "The gate is closed", List.of(id(90)));
        seed(world, first, second);

        SemanticContradictionResult result = SemanticContradictionLifecycle.record(
                world, npc, first.id(), second.id(), 200L, 64);

        assertEquals(SemanticContradictionResult.Status.RECORDED, result.status());
        MemoryEvent event = MemoryEventStore.forWorld(world).findById(npc, result.eventId()).orElseThrow();
        assertTrue(SemanticContradictionPolicy.valid(event));
        assertEquals(first.id(), snapshotFor(event, first).detectedSemanticEntryId());
        assertEquals(second.id(), snapshotFor(event, second).detectedSemanticEntryId());
    }

    @Test
    void invalidMissingSameAndScopeMismatchRequestsFailWithoutWrites() {
        Path world = tempDir.resolve("rejections");
        UUID npc = id(10);
        SemanticMemoryEntry first = belief(id(110), npc, "Road clear", List.of(id(90)));
        SemanticMemoryEntry otherScope = belief(id(111), npc, "Road blocked", List.of(id(91)));
        seed(world, first, otherScope);

        assertEquals(SemanticContradictionResult.Status.REJECTED,
                SemanticContradictionLifecycle.record(null, npc, first.id(), otherScope.id(), 10L, 64).status());
        assertEquals(SemanticContradictionResult.Status.REJECTED,
                SemanticContradictionLifecycle.record(world, null, first.id(), otherScope.id(), 10L, 64).status());
        assertEquals(SemanticContradictionResult.Status.REJECTED,
                SemanticContradictionLifecycle.record(world, npc, null, otherScope.id(), 10L, 64).status());
        assertEquals(SemanticContradictionResult.Status.REJECTED,
                SemanticContradictionLifecycle.record(world, npc, first.id(), otherScope.id(), 10L, 0).status());
        assertEquals(SemanticContradictionResult.Status.SAME_CLAIM,
                SemanticContradictionLifecycle.record(world, npc, first.id(), first.id(), 10L, 64).status());
        assertEquals(SemanticContradictionResult.Status.SOURCE_NOT_RETAINED,
                SemanticContradictionLifecycle.record(world, npc, first.id(), id(999), 10L, 64).status());
        assertEquals(SemanticContradictionResult.Status.SOURCE_NOT_RETAINED,
                SemanticContradictionLifecycle.record(world, id(11), first.id(), otherScope.id(), 10L, 64).status());
        assertEquals(SemanticContradictionResult.Status.SCOPE_MISMATCH,
                SemanticContradictionLifecycle.record(world, npc, first.id(), otherScope.id(), 10L, 64).status());
        assertTrue(MemoryEventStore.forWorld(world).getRecent(npc, 64).isEmpty());
    }

    @Test
    void exactReplayIsByteIdempotentAndLaterDetectionIsDistinct() throws Exception {
        Path world = tempDir.resolve("replay");
        UUID npc = id(20);
        SemanticMemoryEntry first = fact(id(120), npc, "Bridge intact", List.of());
        SemanticMemoryEntry second = belief(id(121), npc, "Bridge destroyed", List.of());
        seed(world, first, second);

        SemanticContradictionResult initial = SemanticContradictionLifecycle.record(
                world, npc, first.id(), second.id(), 300L, 64);
        Path file = world.resolve("livingworld/memory2.json");
        String before = Files.readString(file);

        SemanticContradictionResult replay = SemanticContradictionLifecycle.record(
                world, npc, second.id(), first.id(), 300L, 64);
        String after = Files.readString(file);
        SemanticContradictionResult later = SemanticContradictionLifecycle.record(
                world, npc, first.id(), second.id(), 301L, 64);

        assertEquals(SemanticContradictionResult.Status.RECORDED, initial.status());
        assertEquals(SemanticContradictionResult.Status.RECORDED, replay.status());
        assertEquals(initial.eventId(), replay.eventId());
        assertEquals(before, after);
        assertEquals(SemanticContradictionResult.Status.RECORDED, later.status());
        assertNotEquals(initial.eventId(), later.eventId());
    }

    @Test
    void eventPressureCanRejectEvidenceWithoutMutatingSemanticClaims() {
        Path world = tempDir.resolve("pressure");
        UUID npc = id(30);
        SemanticMemoryEntry first = fact(id(130), npc, "Mine safe", List.of());
        SemanticMemoryEntry second = belief(id(131), npc, "Mine unsafe", List.of());
        seed(world, first, second);
        List<SemanticMemoryEntry> semanticBefore = SemanticMemoryStore.forWorld(world).getRecent(npc, 64);

        MemoryEventStore.forWorld(world).append(new MemoryEvent(
                id(500), npc, MemoryEvent.Type.RELATIONSHIP_CHANGE, "Strong retained event", List.of(npc),
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 1_000L, 0L, 100, 100, 100, List.of()
        ), 1);

        SemanticContradictionResult result = SemanticContradictionLifecycle.record(
                world, npc, first.id(), second.id(), 100L, 1);

        assertEquals(SemanticContradictionResult.Status.EVENT_NOT_RETAINED, result.status());
        assertEquals(semanticBefore, SemanticMemoryStore.forWorld(world).getRecent(npc, 64));
        assertTrue(MemoryEventStore.forWorld(world).findById(npc, result.eventId()).isEmpty());
    }

    private static void seed(Path world, SemanticMemoryEntry... entries) {
        SemanticMemoryStore store = SemanticMemoryStore.forWorld(world);
        for (SemanticMemoryEntry entry : entries) store.append(entry, 64);
    }

    private static SemanticContradiction.ClaimSnapshot snapshotFor(
            MemoryEvent event,
            SemanticMemoryEntry entry
    ) {
        UUID logical = SemanticMemoryIdentity.logicalClaimId(entry);
        SemanticContradiction payload = event.semanticContradiction();
        return payload.first().logicalClaimId().equals(logical) ? payload.first() : payload.second();
    }

    private static SemanticMemoryEntry fact(UUID entryId, UUID owner, String statement, List<UUID> scope) {
        return new SemanticMemoryEntry(
                entryId, owner, SemanticMemoryEntry.Kind.FACT, statement, scope,
                MemoryEvent.Provenance.SYSTEM_OBSERVED, 10L, 0L, 90, 100, List.of(id(900)));
    }

    private static SemanticMemoryEntry belief(UUID entryId, UUID owner, String statement, List<UUID> scope) {
        return new SemanticMemoryEntry(
                entryId, owner, SemanticMemoryEntry.Kind.BELIEF, statement, scope,
                MemoryEvent.Provenance.NPC_TOLD, 10L, 0L, 50, 50, List.of(id(901)));
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
