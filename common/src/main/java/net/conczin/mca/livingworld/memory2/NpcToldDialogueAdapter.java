package net.conczin.mca.livingworld.memory2;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Pure deterministic construction of listener-owned NPC_TOLD transfer evidence. */
final class NpcToldDialogueAdapter {
    private static final String ID_NAMESPACE = "npc-knowledge-transfer-v1";
    private static final String SUMMARY_PREFIX = "NPC told: ";

    private NpcToldDialogueAdapter() {
    }

    static Optional<MemoryEvent> create(
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            long authoritativeGameTime,
            String statement
    ) {
        if (speakerNpcId == null
                || listenerNpcId == null
                || speakerSemanticEntryId == null
                || speakerNpcId.equals(listenerNpcId)) {
            return Optional.empty();
        }

        String normalizedStatement = SemanticMemoryIngestionAdapter.normalizeAndLimitStatement(statement);
        if (normalizedStatement.isBlank()) return Optional.empty();

        long safeGameTime = Math.max(0L, authoritativeGameTime);
        return Optional.of(new MemoryEvent(
                deterministicEvidenceId(speakerNpcId, listenerNpcId, speakerSemanticEntryId, safeGameTime),
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
                null
        ));
    }

    static UUID deterministicEvidenceId(
            UUID speakerNpcId,
            UUID listenerNpcId,
            UUID speakerSemanticEntryId,
            long authoritativeGameTime
    ) {
        String canonical = ID_NAMESPACE
                + '\n' + listenerNpcId
                + '\n' + speakerNpcId
                + '\n' + speakerSemanticEntryId
                + '\n' + Math.max(0L, authoritativeGameTime);
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }
}
