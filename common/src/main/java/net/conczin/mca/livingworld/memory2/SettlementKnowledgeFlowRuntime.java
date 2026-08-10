package net.conczin.mca.livingworld.memory2;

import net.conczin.mca.MCA;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

/** Public Minecraft-runtime adapter for one bounded settlement knowledge-flow cycle. */
public final class SettlementKnowledgeFlowRuntime {
    private SettlementKnowledgeFlowRuntime() {
    }

    public static void runIfEnabled(
            boolean enabled,
            Path worldRoot,
            int villageId,
            long authoritativeGameTime,
            Collection<UUID> residentIds,
            int capacityPerNpc
    ) {
        if (!enabled || worldRoot == null || residentIds == null || residentIds.size() < 2) return;
        try {
            SettlementKnowledgeFlowLifecycle.runCycle(
                    worldRoot,
                    villageId,
                    authoritativeGameTime,
                    residentIds,
                    capacityPerNpc,
                    capacityPerNpc
            );
        } catch (RuntimeException e) {
            MCA.LOGGER.warn(
                    "Unable to run bounded settlement knowledge flow for village {} at gameTime {}",
                    villageId,
                    authoritativeGameTime,
                    e
            );
        }
    }
}
