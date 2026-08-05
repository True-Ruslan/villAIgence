package net.conczin.mca.mixin;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.chatAI.ChatAI;
import net.conczin.mca.livingworld.ai.LivingWorldAI;
import net.conczin.mca.network.AuthenticatedTextTurn;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChat", at = @At("HEAD"))
    public void mca$injectHandleChat(ServerboundChatPacket message, CallbackInfo ci) {
        if (LivingWorldAI.isChatEnabled()) {
            String msg = StringUtils.normalizeSpace(message.message());
            if (!msg.startsWith("/")) {
                // Resolve the target from the authenticated connection player and live server state.
                Optional<VillagerEntityMCA> villager = ChatAI.getVillagerForConversation(player, msg);
                villager.ifPresent(resolved -> AuthenticatedTextTurn.dispatch(player, resolved, msg));
            }
        }
    }
}
