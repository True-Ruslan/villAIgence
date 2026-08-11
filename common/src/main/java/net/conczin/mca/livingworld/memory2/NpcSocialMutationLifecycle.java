package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.livingworld.relationship.NpcSocialCausalMutation;
import net.conczin.mca.livingworld.relationship.NpcSocialDelta;
import net.conczin.mca.livingworld.relationship.NpcSocialGraphStore;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/** Server-owned causal authority for one directed NPC-to-NPC social mutation. */
public final class NpcSocialMutationLifecycle {
    private NpcSocialMutationLifecycle() {
    }

    public static NpcSocialMutationLifecycleResult apply(
            Path worldRoot,
            UUID sourceNpcId,
            UUID targetNpcId,
            UUID causeEventId,
            NpcSocialDelta proposed,
            int maxDeltaPerMutation,
            int maxEventsPerNpc,
            long createdAtEpochMillis,
            NpcIdentityAuthority identities
    ) {
        if (worldRoot == null
                || sourceNpcId == null
                || targetNpcId == null
                || causeEventId == null
                || proposed == null
                || identities == null
                || sourceNpcId.equals(targetNpcId)) {
            return result(NpcSocialMutationLifecycleResult.Status.INVALID_REQUEST, null, null);
        }

        if (!identities.isNpc(sourceNpcId) || !identities.isNpc(targetNpcId)) {
            return result(NpcSocialMutationLifecycleResult.Status.INVALID_NPC, null, null);
        }

        MemoryEventStore memory = MemoryEventStore.forWorld(worldRoot);
        Optional<MemoryEvent> sourceEvent = memory.findById(sourceNpcId, causeEventId);
        if (sourceEvent.isEmpty()) {
            return result(NpcSocialMutationLifecycleResult.Status.SOURCE_NOT_RETAINED, null, null);
        }

        MemoryEvent cause = sourceEvent.get();
        if (!validSourceEvent(cause, sourceNpcId, targetNpcId)) {
            return result(NpcSocialMutationLifecycleResult.Status.INVALID_SOURCE_EVENT, null, null);
        }

        NpcSocialCausalMutation graphMutation = NpcSocialGraphStore.forWorld(worldRoot).applyCausalDelta(
                sourceNpcId,
                targetNpcId,
                cause.id(),
                cause.gameTime(),
                proposed,
                maxDeltaPerMutation
        );

        return switch (graphMutation.status()) {
            case APPLIED -> appendAudit(
                    memory,
                    sourceNpcId,
                    graphMutation,
                    maxEventsPerNpc,
                    createdAtEpochMillis
            );
            case NO_CHANGE -> result(NpcSocialMutationLifecycleResult.Status.NO_CHANGE, graphMutation, null);
            case CAPACITY_REACHED -> result(
                    NpcSocialMutationLifecycleResult.Status.CAPACITY_REACHED,
                    graphMutation,
                    null
            );
            case REPLAYED -> result(NpcSocialMutationLifecycleResult.Status.REPLAYED, graphMutation, null);
            case STALE_CAUSE -> result(NpcSocialMutationLifecycleResult.Status.STALE_CAUSE, graphMutation, null);
            case CONFLICTING_CAUSE -> result(
                    NpcSocialMutationLifecycleResult.Status.CONFLICTING_CAUSE,
                    graphMutation,
                    null
            );
            case FRONTIER_CORRUPT -> result(
                    NpcSocialMutationLifecycleResult.Status.FRONTIER_CORRUPT,
                    graphMutation,
                    null
            );
            case INVALID_PAIR -> result(NpcSocialMutationLifecycleResult.Status.INVALID_REQUEST, graphMutation, null);
        };
    }

    private static boolean validSourceEvent(MemoryEvent cause, UUID sourceNpcId, UUID targetNpcId) {
        return cause != null
                && sourceNpcId.equals(cause.ownerNpcId())
                && cause.provenance() == MemoryEvent.Provenance.SYSTEM_OBSERVED
                && (cause.type() == MemoryEvent.Type.OBSERVATION || cause.type() == MemoryEvent.Type.ACTION)
                && cause.participants().contains(targetNpcId);
    }

    private static NpcSocialMutationLifecycleResult appendAudit(
            MemoryEventStore memory,
            UUID sourceNpcId,
            NpcSocialCausalMutation graphMutation,
            int maxEventsPerNpc,
            long createdAtEpochMillis
    ) {
        Optional<MemoryEvent> candidate = NpcSocialMutationMemoryAdapter.toMemoryEvent(
                sourceNpcId,
                graphMutation,
                createdAtEpochMillis
        );
        if (candidate.isEmpty()) {
            return result(
                    NpcSocialMutationLifecycleResult.Status.APPLIED_AUDIT_NOT_RETAINED,
                    graphMutation,
                    null
            );
        }

        MemoryEvent audit = candidate.get();
        memory.append(audit, maxEventsPerNpc);
        Optional<MemoryEvent> retained = memory.findById(sourceNpcId, audit.id());
        if (retained.isEmpty() || !retained.get().equals(audit)) {
            return result(
                    NpcSocialMutationLifecycleResult.Status.APPLIED_AUDIT_NOT_RETAINED,
                    graphMutation,
                    null
            );
        }

        return result(NpcSocialMutationLifecycleResult.Status.APPLIED, graphMutation, retained.get());
    }

    private static NpcSocialMutationLifecycleResult result(
            NpcSocialMutationLifecycleResult.Status status,
            NpcSocialCausalMutation graphMutation,
            MemoryEvent auditEvent
    ) {
        return new NpcSocialMutationLifecycleResult(status, graphMutation, auditEvent);
    }
}
