package net.conczin.mca.network.c2s;

import net.conczin.mca.MCA;
import net.conczin.mca.network.HandleablePayload;
import net.conczin.mca.network.Network;
import net.conczin.mca.network.s2c.BuildingPolymorphMessage;
import net.conczin.mca.resources.BuildingTypes;
import net.conczin.mca.server.world.data.Building;
import net.conczin.mca.server.world.data.BuildingScanResult;
import net.conczin.mca.server.world.data.Village;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

public record ReportBuildingMessage(Action action, String data) implements HandleablePayload {
    public static final CustomPacketPayload.Type<ReportBuildingMessage> TYPE = new CustomPacketPayload.Type<>(MCA.locate("report_building"));
    public static final StreamCodec<FriendlyByteBuf, ReportBuildingMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal), ReportBuildingMessage::action,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).map(o -> o.orElse(null), o -> o == null ? java.util.Optional.empty() : java.util.Optional.of(o)), ReportBuildingMessage::data,
            ReportBuildingMessage::new
    );

    private static final int BUILDING_LOOKUP_HORIZONTAL_MARGIN = 1;
    private static final int BUILDING_LOOKUP_VERTICAL_MARGIN = 2;

    public ReportBuildingMessage(Action action) {
        this(action, null);
    }

    @Override
    public void handleServer(ServerPlayer player) {
        VillageManager villages = VillageManager.get(player.serverLevel());
        switch (action) {
            case ADD, ADD_ROOM -> {
                BlueprintPermissionPolicy.Operation operation = action == Action.ADD
                        ? BlueprintPermissionPolicy.Operation.ADD_BUILDING
                        : BlueprintPermissionPolicy.Operation.ADD_ROOM;
                Optional<Village> nearest = BlueprintServerAuthority.nearestVillage(player);
                if (nearest.isPresent() && !BlueprintServerAuthority.hasPermission(nearest.get(), player, operation)) {
                    BlueprintServerAuthority.deny(player);
                    return;
                }

                boolean isRoom = action == Action.ADD_ROOM;
                BuildingScanResult scan = villages.analyzeBuilding(player.blockPosition(), isRoom);
                if (scan.result() == Building.validationResult.SUCCESS && scan.isAmbiguous()) {
                    Network.sendToPlayer(new BuildingPolymorphMessage(scan.matchingTypes(), scan.source(), scan.strictScan()), player);
                } else {
                    Building.validationResult result = villages.commitBuilding(scan, null);
                    player.displayClientMessage(Component.translatable("blueprint.scan." + result.name().toLowerCase(Locale.ENGLISH)), true);
                }
            }
            case AUTO_SCAN -> BlueprintServerAuthority.nearestAuthorized(player, BlueprintPermissionPolicy.Operation.TOGGLE_AUTO_SCAN)
                    .ifPresentOrElse(Village::toggleAutoScan, () -> BlueprintServerAuthority.deny(player));
            case FULL_SCAN -> BlueprintServerAuthority.nearestAuthorized(player, BlueprintPermissionPolicy.Operation.FULL_SCAN)
                    .ifPresentOrElse(village -> village.getBuildings().values().stream().toList().forEach(building ->
                                    villages.processBuilding(building.getCenter(), true, building.isStrictScan())),
                            () -> BlueprintServerAuthority.deny(player));
            case FORCE_TYPE, REMOVE -> handleAdministrativeBuildingMutation(player, villages);
        }
    }

    private void handleAdministrativeBuildingMutation(ServerPlayer player, VillageManager villages) {
        BlueprintPermissionPolicy.Operation operation = action == Action.FORCE_TYPE
                ? BlueprintPermissionPolicy.Operation.FORCE_BUILDING_TYPE
                : BlueprintPermissionPolicy.Operation.REMOVE_BUILDING;
        Optional<Village> authorized = BlueprintServerAuthority.nearestAuthorized(player, operation);
        if (authorized.isEmpty()) {
            BlueprintServerAuthority.deny(player);
            return;
        }
        if (action == Action.FORCE_TYPE && (data == null || data.isBlank()
                || !BuildingTypes.getInstance().getServerBuildingTypes().containsKey(data))) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        Village village = authorized.get();
        Building targetBuilding = null;
        boolean targetExact = false;
        double targetDistance = Double.MAX_VALUE;

        for (Building building : village.getBuildings().values()) {
            if (action == Action.FORCE_TYPE && building.getBuildingType().grouped()) continue;

            boolean exact = building.containsPos(playerPos);
            boolean lenient = containsLenient(building, playerPos);
            if (!exact && !lenient) continue;

            double distance = building.getCenter().distSqr(playerPos);
            if (targetBuilding == null
                    || (exact && !targetExact)
                    || (exact == targetExact && distance < targetDistance)) {
                targetBuilding = building;
                targetExact = exact;
                targetDistance = distance;
            }
        }

        if (targetBuilding == null) {
            player.displayClientMessage(Component.translatable("blueprint.noBuilding"), true);
            return;
        }

        if (action == Action.FORCE_TYPE) {
            if (targetBuilding.getType().equals(data)) {
                targetBuilding.setTypeForced(false);
                targetBuilding.determineType();
            } else {
                targetBuilding.setTypeForced(true);
                targetBuilding.setType(data);
            }
            village.markDirty();
        } else {
            village.removeBuilding(targetBuilding.getId());
        }
    }

    private static boolean containsLenient(Building building, BlockPos pos) {
        BlockPos p0 = building.getPos0();
        BlockPos p1 = building.getPos1();

        return pos.getX() >= p0.getX() - BUILDING_LOOKUP_HORIZONTAL_MARGIN && pos.getX() <= p1.getX() + BUILDING_LOOKUP_HORIZONTAL_MARGIN
                && pos.getY() >= p0.getY() - BUILDING_LOOKUP_VERTICAL_MARGIN && pos.getY() <= p1.getY() + BUILDING_LOOKUP_VERTICAL_MARGIN
                && pos.getZ() >= p0.getZ() - BUILDING_LOOKUP_HORIZONTAL_MARGIN && pos.getZ() <= p1.getZ() + BUILDING_LOOKUP_HORIZONTAL_MARGIN;
    }

    @Override
    public CustomPacketPayload.Type<ReportBuildingMessage> type() {
        return TYPE;
    }

    public enum Action {
        AUTO_SCAN,
        ADD_ROOM,
        ADD,
        REMOVE,
        FORCE_TYPE,
        FULL_SCAN
    }
}
