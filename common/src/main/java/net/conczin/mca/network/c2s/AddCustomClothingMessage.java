package net.conczin.mca.network.c2s;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.conczin.mca.Config;
import net.conczin.mca.MCA;
import net.conczin.mca.network.HandleablePayload;
import net.conczin.mca.resources.data.skin.Clothing;
import net.conczin.mca.resources.data.skin.Hair;
import net.conczin.mca.resources.data.skin.SkinListEntry;
import net.conczin.mca.server.world.data.CustomClothingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record AddCustomClothingMessage(String identifier, boolean isHair, String json) implements HandleablePayload {
    private static final int MAX_IDENTIFIER_LENGTH = 256;
    private static final int MAX_JSON_LENGTH = 1 << 16;

    public static final CustomPacketPayload.Type<AddCustomClothingMessage> TYPE = new CustomPacketPayload.Type<>(MCA.locate("add_custom_clothing"));
    public static final StreamCodec<FriendlyByteBuf, AddCustomClothingMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_IDENTIFIER_LENGTH), AddCustomClothingMessage::identifier,
            ByteBufCodecs.BOOL, AddCustomClothingMessage::isHair,
            ByteBufCodecs.stringUtf8(MAX_JSON_LENGTH), AddCustomClothingMessage::json,
            AddCustomClothingMessage::new
    );

    public static AddCustomClothingMessage fromEntry(SkinListEntry entry) {
        boolean hair = entry instanceof Hair;
        JsonObject j = entry.toJson();
        return new AddCustomClothingMessage(entry.getIdentifier(), hair, j.toString());
    }

    private static boolean isAllowed(ServerPlayer player) {
        return player.hasPermissions(4) || Config.getServerConfig().allowEveryoneToAddContentGlobally;
    }

    @Override
    public void handleServer(ServerPlayer player) {
        if (!isAllowed(player)) {
            return;
        }

        JsonObject obj;
        try {
            obj = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException exception) {
            return;
        }

        if (isHair) {
            Hair hair = new Hair(identifier, obj);
            CustomClothingManager.getHair().addEntry(identifier, hair);
        } else {
            Clothing clothing = new Clothing(identifier, obj);
            CustomClothingManager.getClothing().addEntry(identifier, clothing);
        }
    }

    @Override
    public CustomPacketPayload.Type<AddCustomClothingMessage> type() {
        return TYPE;
    }
}
