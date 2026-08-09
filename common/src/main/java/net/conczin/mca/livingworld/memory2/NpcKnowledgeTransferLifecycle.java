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
                || speakerSemanticEntryId == null) {
            return result(NpcKnowledgeTransferResult.Status.REJECTED, null, null);
        }
        if (speakerNpcId.equals(listenerNpcId)) {
            return result(NpcKnowledgeTransferResult.Status.PROVENANCE_CYCLE, null, null);
        }

        int safeMemoryCapacity = Math.max(1, memory2CapacityPerNpc);
        int safeSemanticCapacity = Math.max(1, semanticCapacityPerNpc);
        SemanticMemoryStore semanticStore = SemanticMemoryStore.forWorld(worldRoot);
        MemoryEventStore eventStore = MemoryEventStore.forWorld(worldRoot);

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
        long safeGameTime = Math.max(0L, authoritativeGameTime);
        UUID evidenceId = KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                speakerNpcId,
                listenerNpcId,
                speakerSemanticEntryId,
                safeGameTime
        );

        Optional<KnowledgeTransferProvenance> provenance;
        if (source.kind() == SemanticMemoryEntry.Kind.BELIEF
                && source.provenance() == MemoryEvent.Provenance.NPC_TOLD) {
            Optional<KnowledgeTransferProvenanceResolver.ResolvedSource> resolved =
                    KnowledgeTransferProvenanceResolver.resolve(eventStore, source);
            if (resolved.isEmpty()) {
                return result(NpcKnowledgeTransferResult.Status.PROVENANCE_UNAVAILABLE, null, null);
            }
            KnowledgeTransferProvenance selectedLineage = resolved.get().provenance();
            if (KnowledgeTransferProvenancePolicy.wouldCycle(selectedLineage, listenerNpcId)) {
                return result(NpcKnowledgeTransferResult.Status.PROVENANCE_CYCLE, null, null);
            }
            if (KnowledgeTransferProvenancePolicy.atHopLimit(selectedLineage)) {
                return result(NpcKnowledgeTransferResult.Status.PROVENANCE_LIMIT_REACHED, null, null);
            }
            provenance = KnowledgeTransferProvenanceFactory.appendHop(
                    selectedLineage,
                    source,
                    listenerNpcId,
                    evidenceId,
                    safeGameTime
            );
        } else {
            provenance = KnowledgeTransferProvenanceFactory.firstHop(
                    source,
                    listenerNpcId,
                    evidenceId,
                    safeGameTime
            );
        }
        if (provenance.isEmpty()) {
            return result(NpcKnowledgeTransferResult.Status.REJECTED, null, null);
        }

        Optional<MemoryEvent> constructedEvidence = NpcToldDialogueAdapter.create(
                speakerNpcId,
                listenerNpcId,
                speakerSemanticEntryId,
                safeGameTime,
                normalizedStatement,
                provenance.get()
        );
        if (constructedEvidence.isEmpty()) {
            return result(NpcKnowledgeTransferResult.Status.REJECTED, null, null);
        }
        MemoryEvent evidence = constructedEvidence.get();

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
                safeGameTime,
                normalizedStatement,
                provenance.get()
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
