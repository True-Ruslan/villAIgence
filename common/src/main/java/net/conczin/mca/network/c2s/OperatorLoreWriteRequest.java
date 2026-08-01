package net.conczin.mca.network.c2s;

import net.conczin.mca.MCA;
import net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorPolicy;
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

public record OperatorLoreWriteRequest(
        int requestId,
        String scope,
        int villagerEntityId,
        String expectedRevision,
        String value
) implements HandleablePayload {
    public static final CustomPacketPayload.Type<OperatorLoreWriteRequest> TYPE =
            new CustomPacketPayload.Type<>(MCA.locate("operator_lore_write"));
    public static final StreamCodec<FriendlyByteBuf, OperatorLoreWriteRequest> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, OperatorLoreWriteRequest::requestId,
            ByteBufCodecs.stringUtf8(16), OperatorLoreWriteRequest::scope,
            ByteBufCodecs.INT, OperatorLoreWriteRequest::villagerEntityId,
            ByteBufCodecs.stringUtf8(OperatorLoreEditorPolicy.REVISION_HEX_LENGTH), OperatorLoreWriteRequest::expectedRevision,
            ByteBufCodecs.stringUtf8(OperatorLoreEditorPolicy.MAX_CODE_POINTS), OperatorLoreWriteRequest::value,
            OperatorLoreWriteRequest::new
    );

    @Override
    public void handleServer(ServerPlayer player) {
        OperatorLoreEditorResult result = OperatorLoreServerAuthority.write(
                player,
                scope,
                villagerEntityId,
                expectedRevision,
                value
        );
        Network.sendToPlayer(
                new OperatorLoreResponse(OperatorLoreProtocolPolicy.echo(requestId), result),
                player
        );
    }

    @Override
    public CustomPacketPayload.Type<OperatorLoreWriteRequest> type() {
        return TYPE;
    }
}
