package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.List;
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
        return transferInternal(
                worldRoot,
                speakerNpcId,
                listenerNpcId,
                speakerSemanticEntryId,
                authoritativeGameTime,
                memory2CapacityPerNpc,
                semanticCapacityPerNpc,
                false
        );
    }

    public static NpcKnowledgeTransferResult transferOmittingTrailingSentence(
            Path worldRoot,
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            long authoritativeGameTime,
            int memory2CapacityPerNpc,
            int semanticCapacityPerNpc
    ) {
        return transferInternal(
                worldRoot,
                speakerNpcId,
                listenerNpcId,
                speakerSemanticEntryId,
                authoritativeGameTime,
                memory2CapacityPerNpc,
                semanticCapacityPerNpc,
                true
        );
    }

    private static NpcKnowledgeTransferResult transferInternal(
            Path worldRoot,
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            long authoritativeGameTime,
            int memory2CapacityPerNpc,
            int semanticCapacityPerNpc,
            boolean omitTrailingSentence
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
        KnowledgeTransferTransformation inheritedTransformation = null;
        if (source.kind() == SemanticMemoryEntry.Kind.BELIEF
                && source.provenance() == MemoryEvent.Provenance.NPC_TOLD) {
            Optional<KnowledgeTransferProvenanceResolver.ResolvedSource> resolved =
                    KnowledgeTransferProvenanceResolver.resolve(eventStore, source);
            if (resolved.isEmpty()) {
                return result(NpcKnowledgeTransferResult.Status.PROVENANCE_UNAVAILABLE, null, null);
            }
            KnowledgeTransferProvenance selectedLineage = resolved.get().provenance();
            inheritedTransformation = resolved.get().transformation();
            if (KnowledgeTransferProvenancePolicy.wouldCycle(selectedLineage, listenerNpcId)) {
                return result(NpcKnowledgeTransferResult.Status.PROVENANCE_CYCLE, null, null);
            }
            if (KnowledgeTransferProvenancePolicy.atHopLimit(selectedLineage)) {
                return result(NpcKnowledgeTransferResult.Status.PROVENANCE_LIMIT_REACHED, null, null);
            }
            if (omitTrailingSentence && inheritedTransformation != null) {
                return result(
                        NpcKnowledgeTransferResult.Status.TRANSFORMATION_LIMIT_REACHED,
                        null,
                        null
                );
            }
            provenance = KnowledgeTransferProvenanceFactory.appendHop(
                    selectedLineage,
                    source,
                    listenerNpcId,
                    evidenceId,
                    safeGameTime,
                    inheritedTransformation
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

        KnowledgeTransferTransformation effectiveTransformation = inheritedTransformation;
        String effectiveStatement = normalizedStatement;
        if (omitTrailingSentence) {
            Optional<String> transformed = KnowledgeTransferTransformationPolicy.omitTrailingSentence(
                    normalizedStatement
            );
            if (transformed.isEmpty()) {
                return result(
                        NpcKnowledgeTransferResult.Status.TRANSFORMATION_NOT_APPLICABLE,
                        null,
                        null
                );
            }
            effectiveStatement = transformed.get();
            effectiveTransformation = new KnowledgeTransferTransformation(List.of(
                    new KnowledgeTransferTransformation.Step(
                            KnowledgeTransferTransformation.Kind.OMIT_TRAILING_SENTENCE,
                            normalizedStatement,
                            effectiveStatement,
                            speakerNpcId,
                            listenerNpcId,
                            speakerSemanticEntryId,
                            evidenceId,
                            safeGameTime
                    )
            ));
            if (!KnowledgeTransferTransformationPolicy.valid(
                    effectiveTransformation,
                    provenance.get()
            )) {
                return result(NpcKnowledgeTransferResult.Status.REJECTED, null, null);
            }
        }

        Optional<MemoryEvent> constructedEvidence = NpcToldDialogueAdapter.create(
                speakerNpcId,
                listenerNpcId,
                speakerSemanticEntryId,
                safeGameTime,
                effectiveStatement,
                provenance.get(),
                effectiveTransformation
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
                effectiveStatement,
                provenance.get(),
                effectiveTransformation
        )) {
            return result(NpcKnowledgeTransferResult.Status.REJECTED, evidence.id(), null);
        }

        Optional<SemanticBeliefSource> admitted = SemanticBeliefAdmissionPolicy.admit(
                persistedEvidence.get(),
                effectiveStatement,
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
