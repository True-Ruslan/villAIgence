package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/** Server-owned orchestration boundary for one NPC-to-NPC knowledge-transfer attempt. */
public final class NpcKnowledgeTransferLifecycle {
    private NpcKnowledgeTransferLifecycle() {
    }

    public static NpcKnowledgeTransferResult transfer(
            Path worldRoot,
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            long authoritativeGameTime,
            int memory2CapacityPerNpc,
            int semanticCapacityPerNpc
    ) {
        if (worldRoot == null
                || speakerNpcId == null
                || listenerNpcId == null
                || speakerSemanticEntryId == null
                || speakerNpcId.equals(listenerNpcId)) {
            return result(NpcKnowledgeTransferResult.Status.REJECTED, null, null);
        }

        int safeMemoryCapacity = Math.max(1, memory2CapacityPerNpc);
        int safeSemanticCapacity = Math.max(1, semanticCapacityPerNpc);
        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(worldRoot);

        Optional<SemanticMemoryEntry> firstLookup = semanticStore.findById(
                speakerNpcId,
                speakerSemanticEntryId
        );
        if (firstLookup.isEmpty()
                || !NpcKnowledgeTransferPolicy.validRequest(
                speakerNpcId,
                listenerNpcId,
                firstLookup.get()
        )) {
            return result(NpcKnowledgeTransferResult.Status.REJECTED, null, null);
        }
        SemanticMemoryEntry selected = firstLookup.get();

        Optional<SemanticMemoryEntry> reread = semanticStore.findById(
                speakerNpcId,
                speakerSemanticEntryId
        );
        if (reread.isEmpty()
                || !NpcKnowledgeTransferPolicy.sameSourceSnapshot(selected, reread.get())) {
            return result(NpcKnowledgeTransferResult.Status.REJECTED, null, null);
        }
        SemanticMemoryEntry source = reread.get();
        String normalizedStatement = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(
                source.statement()
        );

        Optional<MemoryEvent> constructedEvidence = NpcToldDialogueAdapter.create(
                speakerNpcId,
                listenerNpcId,
                speakerSemanticEntryId,
                authoritativeGameTime,
                normalizedStatement
        );
        if (constructedEvidence.isEmpty()) {
            return result(NpcKnowledgeTransferResult.Status.REJECTED, null, null);
        }
        MemoryEvent evidence = constructedEvidence.get();

        MemoryEventStore eventStore = MemoryEventStore.forWorld(worldRoot);
        eventStore.append(evidence, safeMemoryCapacity);
        Optional<MemoryEvent> persistedEvidence = eventStore.findById(listenerNpcId, evidence.id());
        if (persistedEvidence.isEmpty()) {
            return result(
                    NpcKnowledgeTransferResult.Status.SOURCE_NOT_RETAINED,
                    evidence.id(),
                    null
            );
        }
        if (!NpcKnowledgeTransferPolicy.validEvidence(
                persistedEvidence.get(),
                speakerNpcId,
                listenerNpcId,
                speakerSemanticEntryId,
                authoritativeGameTime,
                normalizedStatement
        )) {
            return result(NpcKnowledgeTransferResult.Status.REJECTED, evidence.id(), null);
        }

        Optional<SemanticBeliefSource> admitted = SemanticBeliefAdmissionPolicy.admit(
                persistedEvidence.get(),
                normalizedStatement,
                source.relatedEntities(),
                MemoryEvent.Provenance.NPC_TOLD,
                50,
                50
        );
        if (admitted.isEmpty()) {
            return result(NpcKnowledgeTransferResult.Status.REJECTED, evidence.id(), null);
        }

        SemanticMemoryEntry expectedCandidate = SemanticMemoryIngestionAdapter.toBelief(admitted.get());
        ControlledSemanticMemoryIngestor.recordBelief(
                worldRoot,
                admitted.get(),
                safeSemanticCapacity
        );

        Optional<SemanticMemoryEntry> retained = semanticStore.findMatching(
                listenerNpcId,
                candidate -> NpcKnowledgeTransferPolicy.compatibleRetainedBelief(
                        expectedCandidate,
                        candidate,
                        evidence.id()
                )
        );
        if (retained.isEmpty()) {
            return result(
                    NpcKnowledgeTransferResult.Status.BELIEF_NOT_RETAINED,
                    evidence.id(),
                    null
            );
        }

        return result(
                NpcKnowledgeTransferResult.Status.ADMITTED,
                evidence.id(),
                retained.get().id()
        );
    }

    private static NpcKnowledgeTransferResult result(
            NpcKnowledgeTransferResult.Status status,
            UUID evidenceEventId,
            UUID semanticEntryId
    ) {
        return new NpcKnowledgeTransferResult(status, evidenceEventId, semanticEntryId);
    }
}
