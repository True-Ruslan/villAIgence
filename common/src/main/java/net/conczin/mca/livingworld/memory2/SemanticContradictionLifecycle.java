package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Server-owned exact-source lifecycle for recording semantic contradiction process evidence. */
public final class SemanticContradictionLifecycle {
    private SemanticContradictionLifecycle() {
    }

    public static SemanticContradictionResult record(
            Path worldRoot,
            UUID npcId,
            UUID firstSemanticEntryId,
            UUID secondSemanticEntryId,
            long authoritativeGameTime,
            int maxEventsPerNpc
    ) {
        if (worldRoot == null
                || npcId == null
                || firstSemanticEntryId == null
                || secondSemanticEntryId == null
                || maxEventsPerNpc <= 0) {
            return result(SemanticContradictionResult.Status.REJECTED, null);
        }
        if (firstSemanticEntryId.equals(secondSemanticEntryId)) {
            return result(SemanticContradictionResult.Status.SAME_CLAIM, null);
        }

        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(worldRoot);
        Optional<SemanticMemoryEntry> firstRead = semanticStore.findById(npcId, firstSemanticEntryId);
        Optional<SemanticMemoryEntry> secondRead = semanticStore.findById(npcId, secondSemanticEntryId);
        if (firstRead.isEmpty() || secondRead.isEmpty()) {
            return result(SemanticContradictionResult.Status.SOURCE_NOT_RETAINED, null);
        }

        Optional<SemanticMemoryEntry> authoritativeFirst = semanticStore.findById(npcId, firstSemanticEntryId);
        Optional<SemanticMemoryEntry> authoritativeSecond = semanticStore.findById(npcId, secondSemanticEntryId);
        if (authoritativeFirst.isEmpty()
                || authoritativeSecond.isEmpty()
                || !authoritativeFirst.get().equals(firstRead.get())
                || !authoritativeSecond.get().equals(secondRead.get())) {
            return result(SemanticContradictionResult.Status.SOURCE_NOT_RETAINED, null);
        }

        SemanticMemoryEntry first = authoritativeFirst.get();
        SemanticMemoryEntry second = authoritativeSecond.get();
        if (SemanticMemoryIdentity.logicalClaimId(first).equals(SemanticMemoryIdentity.logicalClaimId(second))) {
            return result(SemanticContradictionResult.Status.SAME_CLAIM, null);
        }
        List<UUID> firstScope = SemanticMemoryIdentity.canonicalIds(first.relatedEntities());
        List<UUID> secondScope = SemanticMemoryIdentity.canonicalIds(second.relatedEntities());
        if (!firstScope.equals(secondScope)) {
            return result(SemanticContradictionResult.Status.SCOPE_MISMATCH, null);
        }

        Optional<MemoryEvent> candidate = SemanticContradictionAdapter.create(
                first,
                second,
                authoritativeGameTime
        );
        if (candidate.isEmpty()) {
            return result(SemanticContradictionResult.Status.REJECTED, null);
        }

        MemoryEvent expected = candidate.get();
        MemoryEventStore eventStore = MemoryEventStore.forWorld(worldRoot);
        eventStore.append(expected, maxEventsPerNpc);
        Optional<MemoryEvent> persisted = eventStore.findById(npcId, expected.id());
        if (persisted.isEmpty()) {
            return result(SemanticContradictionResult.Status.EVENT_NOT_RETAINED, expected.id());
        }
        if (!persisted.get().equals(expected) || !SemanticContradictionPolicy.valid(persisted.get())) {
            return result(SemanticContradictionResult.Status.REJECTED, expected.id());
        }
        return result(SemanticContradictionResult.Status.RECORDED, persisted.get().id());
    }

    private static SemanticContradictionResult result(
            SemanticContradictionResult.Status status,
            UUID eventId
    ) {
        return new SemanticContradictionResult(status, eventId);
    }
}
