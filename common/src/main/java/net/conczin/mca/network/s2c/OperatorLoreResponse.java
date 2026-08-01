package net.conczin.mca.network.s2c;

import net.conczin.mca.MCA;
import net.conczin.mca.livingworld.lore.OperatorLoreScope;
import net.conczin.mca.livingworld.lore.editor.OperatorLoreClientState;
import net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorPolicy;
import net.conczin.mca.livingworld.lore.editor.OperatorLoreEditorResult;
import net.conczin.mca.network.HandleablePayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

public record OperatorLoreResponse(
        int requestId,
        String scope,
        int villagerEntityId,
        String status,
        String value,
        String revision
) implements HandleablePayload {
    public static final CustomPacketPayload.Type<OperatorLoreResponse> TYPE =
            new CustomPacketPayload.Type<>(MCA.locate("operator_lore_response"));
    public static final StreamCodec<FriendlyByteBuf, OperatorLoreResponse> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, OperatorLoreResponse::requestId,
            ByteBufCodecs.stringUtf8(16), OperatorLoreResponse::scope,
            ByteBufCodecs.INT, OperatorLoreResponse::villagerEntityId,
            ByteBufCodecs.stringUtf8(16), OperatorLoreResponse::status,
            ByteBufCodecs.stringUtf8(OperatorLoreEditorPolicy.MAX_CODE_POINTS), OperatorLoreResponse::value,
            ByteBufCodecs.stringUtf8(OperatorLoreEditorPolicy.REVISION_HEX_LENGTH), OperatorLoreResponse::revision,
            OperatorLoreResponse::new
    );

    public OperatorLoreResponse(int requestId, OperatorLoreEditorResult result) {
        this(
                requestId,
                result.scope().name(),
                result.villagerEntityId(),
                result.status().name(),
                result.value(),
                result.revision()
        );
    }

    @Override
    public void handle(Player player) {
        OperatorLoreClientState.accept(toResult());
    }

    public OperatorLoreEditorResult toResult() {
        return new OperatorLoreEditorResult(
                parseScope(scope),
                villagerEntityId,
                parseStatus(status),
                value,
                revision
        );
    }

    private static OperatorLoreScope parseScope(String value) {
        try {
            return OperatorLoreScope.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException invalid) {
            return OperatorLoreScope.WORLD;
        }
    }

    private static OperatorLoreEditorResult.Status parseStatus(String value) {
        try {
            return OperatorLoreEditorResult.Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException invalid) {
            return OperatorLoreEditorResult.Status.ERROR;
        }
    }

    @Override
    public CustomPacketPayload.Type<OperatorLoreResponse> type() {
        return TYPE;
    }
}
