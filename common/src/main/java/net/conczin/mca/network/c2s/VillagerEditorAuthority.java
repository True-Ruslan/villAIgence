package net.conczin.mca.network.c2s;

import net.conczin.mca.entity.VillagerLike;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the entity targeted by a client-supplied UUID for the villager editor and related
 * data-fetch packets, and applies the same authority every caller must hold: editing/reading a
 * player's own MCA data is self-only (there is no server-side flow that opens the editor on
 * another player - see Command#editor), while a villager target must actually be present, alive
 * and close enough to the requesting player to have been the thing they interacted with.
 */
final class VillagerEditorAuthority {
    static final double MAX_DISTANCE_SQUARED = 64.0 * 64.0;

    private VillagerEditorAuthority() {
    }

    static Optional<Entity> resolve(ServerPlayer player, UUID uuid) {
        Entity entity = player.serverLevel().getEntity(uuid);
        if (entity == null || entity.isRemoved()) {
            return Optional.empty();
        }

        if (entity instanceof ServerPlayer targetPlayer) {
            return targetPlayer.getUUID().equals(player.getUUID()) ? Optional.of(entity) : Optional.empty();
        }

        if (!(entity instanceof VillagerLike<?>) || !entity.isAlive() || entity.level() != player.level()) {
            return Optional.empty();
        }

        if (player.distanceToSqr(entity) > MAX_DISTANCE_SQUARED) {
            return Optional.empty();
        }

        return Optional.of(entity);
    }
}
