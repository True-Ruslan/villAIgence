package net.conczin.mca.network.c2s;

import net.conczin.mca.MCA;
import net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult;
import net.conczin.mca.livingworld.lore.editor.OperatorLoreProtocolPolicy;
import net.conczin.mca.livingworld.lore.editor.OperatorLoreServerAuthority;
import net.conczin.mca.network.HandleablePayload;
import net.conczin.mca.network.Network;
import net.conczin.mca.network.s2c.OperatorLoreResponse;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record OperatorLoreReadRequest(
        int requestId,
        String scope,
        int villagerEntityId
) implements HandleablePayload {
    public static final CustomPacketPayload.Type<OperatorLoreReadRequest> TYPE =
            new CustomPacketPayload.Type<>(MCA.locate("operator_lore_read"));
    public static final StreamCodec<FriendlyByteBuf, OperatorLoreReadRequest> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, OperatorLoreReadRequest::requestId,
            ByteBufCodecs.stringUtf8(16), OperatorLoreReadRequest::scope,
            ByteBufCodecs.INT, OperatorLoreReadRequest::villagerEntityId,
            OperatorLoreReadRequest::new
    );

    @Override
    public void handleServer(ServerPlayer player) {
        OperatorLoreEditorResult result = OperatorLoreServerAuthority.read(player, scope, villagerEntityId);
        Network.sendToPlayer(
                new OperatorLoreResponse(OperatorLoreProtocolPolicy.echo(requestId), result),
                player
        );
    }

    @Override
    public CustomPacketPayload.Type<OperatorLoreReadRequest> type() {
        return TYPE;
    }
}
