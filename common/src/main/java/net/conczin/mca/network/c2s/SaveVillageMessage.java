package net.conczin.mca.network.c2s;

import net.conczin.mca.MCA;
import net.conczin.mca.network.HandleablePayload;
import net.conczin.mca.resources.Rank;
import net.conczin.mca.server.world.data.Village;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record SaveVillageMessage(
        int id,
        float taxes,
        float populationThreshold,
        float marriageThreshold
) implements HandleablePayload {
    public static final CustomPacketPayload.Type<SaveVillageMessage> TYPE = new CustomPacketPayload.Type<>(MCA.locate("save_village"));
    public static final StreamCodec<FriendlyByteBuf, SaveVillageMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SaveVillageMessage::id,
            ByteBufCodecs.FLOAT, SaveVillageMessage::taxes,
            ByteBufCodecs.FLOAT, SaveVillageMessage::populationThreshold,
            ByteBufCodecs.FLOAT, SaveVillageMessage::marriageThreshold,
            SaveVillageMessage::new
    );

    public SaveVillageMessage(Village village) {
        this(village.getId(), village.getTaxes(), village.getPopulationThreshold(), village.getMarriageThreshold());
    }

    @Override
    public void handleServer(ServerPlayer player) {
        if (!BlueprintPermissionPolicy.isValidRatio(taxes)
                || !BlueprintPermissionPolicy.isValidRatio(populationThreshold)
                || !BlueprintPermissionPolicy.isValidRatio(marriageThreshold)) {
            return;
        }

        BlueprintServerAuthority.requestedVillage(player, id).ifPresentOrElse(village -> {
            Rank rank = BlueprintServerAuthority.rank(village, player);
            boolean taxesChanged = Float.compare(village.getTaxes(), taxes) != 0;
            boolean populationChanged = Float.compare(village.getPopulationThreshold(), populationThreshold) != 0;
            boolean marriageChanged = Float.compare(village.getMarriageThreshold(), marriageThreshold) != 0;

            if ((taxesChanged && !BlueprintPermissionPolicy.can(rank, BlueprintPermissionPolicy.Operation.CHANGE_TAXES))
                    || (populationChanged && !BlueprintPermissionPolicy.can(rank, BlueprintPermissionPolicy.Operation.CHANGE_POPULATION))
                    || (marriageChanged && !BlueprintPermissionPolicy.can(rank, BlueprintPermissionPolicy.Operation.CHANGE_MARRIAGE))) {
                BlueprintServerAuthority.deny(player);
                return;
            }

            if (taxesChanged) village.setTaxes(taxes);
            if (populationChanged) village.setPopulationThreshold(populationThreshold);
            if (marriageChanged) village.setMarriageThreshold(marriageThreshold);
        }, () -> BlueprintServerAuthority.deny(player));
    }

    @Override
    public Type<SaveVillageMessage> type() {
        return TYPE;
    }
}
