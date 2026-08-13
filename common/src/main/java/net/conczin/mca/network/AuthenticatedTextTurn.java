package net.conczin.mca.network;

import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.chatAI.ChatAI;
import net.conczin.mca.livingworld.context.LivingWorldContextCapture;
import net.conczin.mca.livingworld.context.LivingWorldContextSnapshot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Server-owned Minecraft bridge for one authenticated text turn. */
public final class AuthenticatedTextTurn {
    private AuthenticatedTextTurn() {
    }

    /**
     * Dispatches one text turn using the production ChatAI and conversation response path.
     * The caller must resolve {@code villager} from trusted server state before invoking this method.
     */
    public static void dispatch(
            ServerPlayer player,
            VillagerEntityMCA villager,
            String message
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(villager, "villager");

        AuthenticatedTextTurnCore.Session session = new AuthenticatedTextTurnCore.Session(
                player.getUUID(),
                villager.getUUID()
        );
        String normalizedMessage = AuthenticatedTextTurnCore.normalize(message);
        if (!AuthenticatedTextTurnCore.isEligibleMessage(normalizedMessage)) return;

        LivingWorldContextSnapshot snapshot = LivingWorldContextCapture.capture(player, villager, normalizedMessage);
        MinecraftServer server = player.serverLevel().getServer();

        CompletableFuture.runAsync(() -> AuthenticatedTextTurnCore.execute(
                session,
                normalizedMessage,
                (ignored, text) -> ChatAI.answer(server, player, villager, text, snapshot),
                (ignored, response) -> villager.conversationManager.addMessage(
                        player,
                        Component.literal(response)
                )
        )).exceptionally(failure -> {
            MCA.LOGGER.warn(
                    "Authenticated text turn failed for player {} and villager {}",
                    session.authenticatedPlayerId(),
                    session.resolvedNpcId(),
                    failure
            );
            return null;
        });
    }
}
