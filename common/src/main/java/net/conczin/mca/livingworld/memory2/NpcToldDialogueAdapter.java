package net.conczin.mca.livingworld.memory2;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Pure deterministic construction of listener-owned NPC_TOLD transfer evidence. */
final class NpcToldDialogueAdapter {
    private static final String SUMMARY_PREFIX = "NPC told: ";

    private NpcToldDialogueAdapter() {
    }

    static Optional<MemoryEvent> create(
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            long authoritativeGameTime,
            String statement,
            KnowledgeTransferProvenance provenance
    ) {
        if (speakerNpcId == null
                || listenerNpcId == null
                || speakerSemanticEntryId == null
                || speakerNpcId.equals(listenerNpcId)
                || !KnowledgeTransferProvenancePolicy.valid(provenance)) {
            return Optional.empty();
        }

        String normalizedStatement = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(statement);
        if (normalizedStatement.isBlank()
                || !normalizedStatement.equals(provenance.origin().statement())) {
            return Optional.empty();
        }

        long safeGameTime = Math.max(0L, authoritativeGameTime);
        UUID evidenceId = deterministicEvidenceId(
                speakerNpcId,
                listenerNpcId,
                speakerSemanticEntryId,
                safeGameTime
        );
        KnowledgeTransferProvenance.Hop lastHop = provenance.hops().getLast();
        if (!speakerNpcId.equals(lastHop.speakerNpcId())
                || !listenerNpcId.equals(lastHop.listenerNpcId())
                || !speakerSemanticEntryId.equals(lastHop.speakerSemanticEntryId())
                || !evidenceId.equals(lastHop.evidenceEventId())
                || safeGameTime != lastHop.gameTime()) {
            return Optional.empty();
        }

        return Optional.of(new MemoryEvent(
                evidenceId,
                listenerNpcId,
                MemoryEvent.Type.DIALOGUE,
                SUMMARY_PREFIX + normalizedStatement,
                List.of(listenerNpcId, speakerNpcId),
                MemoryEvent.Provenance.NPC_TOLD,
                safeGameTime,
                0L,
                50,
                0,
                50,
                List.of(),
                null,
                null,
                null,
                provenance
        ));
    }

    static UUID deterministicEvidenceId(
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            long authoritativeGameTime
    ) {
        return KnowledgeTransferProvenancePolicy.deterministicEvidenceId(
                speakerNpcId,
                listenerNpcId,
                speakerSemanticEntryId,
                authoritativeGameTime
        );
    }
}
