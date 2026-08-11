package net.conczin.mca.livingworld.relationship;

import java.nio.file.Path;
import java.util.UUID;

/** Fresh server-side authorization for provider-proposed snapshot commands. */
public final class SnapshotCommandRelationshipPolicy {
    private SnapshotCommandRelationshipPolicy() {
    }

    public static boolean isAllowed(
            boolean relationshipStateEnabled,
            Path worldRoot,
            UUID villagerId,
            UUID playerId,
            String commandName
    ) {
        if (!relationshipStateEnabled) return true;
        if (!"follow-player".equals(commandName)) return true;
        if (worldRoot == null || villagerId == null || playerId == null) return false;

        try {
            LivingWorldRelationshipState fresh = LivingWorldRelationshipStore.readStrict(
                    worldRoot,
                    villagerId,
                    playerId
            );
            return LivingWorldRelationshipActionPolicy.isAllowed(commandName, fresh);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
