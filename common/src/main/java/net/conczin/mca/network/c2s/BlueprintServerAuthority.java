package net.conczin.mca.network.c2s;

import net.conczin.mca.resources.Rank;
import net.conczin.mca.resources.Tasks;
import net.conczin.mca.server.world.data.Village;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Resolves blueprint authority from server-owned player/village state. */
final class BlueprintServerAuthority {
    private BlueprintServerAuthority() {
    }

    static Optional<Village> nearestAuthorized(
            ServerPlayer player,
            BlueprintPermissionPolicy.Operation operation
    ) {
        return VillageManager.get(player.serverLevel()).findNearestVillage(player)
                .filter(village -> hasPermission(village, player, operation));
    }

    static Optional<Village> requestedAuthorized(
            ServerPlayer player,
            int requestedVillageId,
            BlueprintPermissionPolicy.Operation operation
    ) {
        return VillageManager.get(player.serverLevel()).findNearestVillage(player)
                .filter(village -> village.getId() == requestedVillageId)
                .filter(village -> hasPermission(village, player, operation));
    }

    static boolean hasPermission(
            Village village,
            ServerPlayer player,
            BlueprintPermissionPolicy.Operation operation
    ) {
        Rank rank = Tasks.getRank(village, player);
        return BlueprintPermissionPolicy.can(rank, operation);
    }

    static void deny(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("gui.blueprint.rankTooLow"), true);
    }
}
