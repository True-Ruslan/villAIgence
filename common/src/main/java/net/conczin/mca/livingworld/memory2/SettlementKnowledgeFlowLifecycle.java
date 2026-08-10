package net.conczin.mca.livingworld.memory2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Executes one bounded settlement knowledge-flow cycle through the existing transfer lifecycle. */
final class SettlementKnowledgeFlowLifecycle {
    private SettlementKnowledgeFlowLifecycle() {
    }

    static CycleResult runCycle(
            Path worldRoot,
            int villageId,
            long gameTime,
            Collection<UUID> residentIds,
            int maxEventsPerNpc,
            int maxSemanticEntriesPerNpc
    ) {
        if (worldRoot == null
                || residentIds == null
                || residentIds.size() < 2
                || maxEventsPerNpc <= 0
                || maxSemanticEntriesPerNpc <= 0) {
            return CycleResult.empty();
        }

        SettlementKnowledgeFlowSelector.SelectionResult selection =
                SettlementKnowledgeFlowSelector.select(
                        SemanticMemoryStore.forWorld(worldRoot),
                        villageId,
                        gameTime,
                        residentIds
                );
        if (selection.opportunities().isEmpty()) {
            return new CycleResult(
                    selection.residentWindow().size(),
                    selection.speakersConsidered(),
                    0,
                    0,
                    0,
                    List.of()
            );
        }

        List<NpcKnowledgeTransferResult.Status> statuses = new ArrayList<>(selection.opportunities().size());
        int attempted = 0;
        int admitted = 0;
        for (SettlementKnowledgeFlowSelector.Opportunity opportunity : selection.opportunities()) {
            attempted++;
            NpcKnowledgeTransferResult result = NpcKnowledgeTransferLifecycle.transfer(
                    worldRoot,
                    opportunity.speakerNpcId(),
                    opportunity.listenerNpcId(),
                    opportunity.sourceSemanticEntryId(),
                    Math.max(0L, gameTime),
                    Math.max(1, maxEventsPerNpc),
                    Math.max(1, maxSemanticEntriesPerNpc)
            );
            statuses.add(result.status());
            if (result.status() == NpcKnowledgeTransferResult.Status.ADMITTED) admitted++;
        }

        return new CycleResult(
                selection.residentWindow().size(),
                selection.speakersConsidered(),
                selection.opportunities().size(),
                attempted,
                admitted,
                statuses
        );
    }

    record CycleResult(
            int residentWindowSize,
            int speakersConsidered,
            int opportunities,
            int attemptedTransfers,
            int successfulTransfers,
            List<NpcKnowledgeTransferResult.Status> statuses
    ) {
        CycleResult {
            residentWindowSize = Math.max(0,
                    Math.min(SettlementKnowledgeFlowSelector.MAX_RESIDENTS_PER_CYCLE, residentWindowSize));
            speakersConsidered = Math.max(0,
                    Math.min(SettlementKnowledgeFlowSelector.MAX_SPEAKERS_PER_CYCLE, speakersConsidered));
            opportunities = Math.max(0,
                    Math.min(SettlementKnowledgeFlowSelector.MAX_OPPORTUNITIES_PER_CYCLE, opportunities));
            attemptedTransfers = Math.max(0,
                    Math.min(SettlementKnowledgeFlowSelector.MAX_OPPORTUNITIES_PER_CYCLE, attemptedTransfers));
            successfulTransfers = Math.max(0, Math.min(attemptedTransfers, successfulTransfers));
            statuses = statuses == null ? List.of() : List.copyOf(statuses);
            if (statuses.size() != attemptedTransfers) {
                throw new IllegalArgumentException("status count must equal attempted transfers");
            }
        }

        static CycleResult empty() {
            return new CycleResult(0, 0, 0, 0, 0, List.of());
        }
    }
}
