package net.conczin.mca.network.c2s;

import net.conczin.mca.MCA;
import net.conczin.mca.network.HandleablePayload;
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

public record ConfirmBuildingPolymorphMessage(BlockPos source, boolean strictScan, String chosenType) implements HandleablePayload {
    public static final CustomPacketPayload.Type<ConfirmBuildingPolymorphMessage> TYPE = new CustomPacketPayload.Type<>(MCA.locate("confirm_building_polymorph"));
    private static final double MAX_SOURCE_DISTANCE_SQR = 16.0D * 16.0D;

    private static final StreamCodec<FriendlyByteBuf, BlockPos> BLOCK_POS_CODEC = StreamCodec.of(
            (buf, pos) -> buf.writeBlockPos(pos), buf -> buf.readBlockPos()
    );

    public static final StreamCodec<FriendlyByteBuf, ConfirmBuildingPolymorphMessage> STREAM_CODEC = StreamCodec.composite(
            BLOCK_POS_CODEC, ConfirmBuildingPolymorphMessage::source,
            ByteBufCodecs.BOOL, ConfirmBuildingPolymorphMessage::strictScan,
            ByteBufCodecs.STRING_UTF8, ConfirmBuildingPolymorphMessage::chosenType,
            ConfirmBuildingPolymorphMessage::new
    );

    @Override
    public void handleServer(ServerPlayer player) {
        if (source.distSqr(player.blockPosition()) > MAX_SOURCE_DISTANCE_SQR) return;
        if (chosenType == null || chosenType.isBlank()
                || !BuildingTypes.getInstance().getServerBuildingTypes().containsKey(chosenType)) {
            return;
        }

        Optional<Village> nearest = BlueprintServerAuthority.nearestVillage(player);
        if (nearest.isPresent()
                && !BlueprintServerAuthority.hasPermission(nearest.get(), player, BlueprintPermissionPolicy.Operation.ADD_BUILDING)) {
            BlueprintServerAuthority.deny(player);
            return;
        }

        VillageManager villages = VillageManager.get(player.serverLevel());
        BuildingScanResult scan = villages.analyzeBuilding(source, strictScan);
        Building.validationResult result = scan.result() == Building.validationResult.SUCCESS && scan.matchesType(chosenType)
                ? villages.commitBuilding(scan, chosenType)
                : Building.validationResult.INVALID_TYPE;
        player.displayClientMessage(Component.translatable("blueprint.scan." + result.name().toLowerCase(Locale.ENGLISH)), true);
    }

    @Override
    public CustomPacketPayload.Type<ConfirmBuildingPolymorphMessage> type() {
        return TYPE;
    }
}
