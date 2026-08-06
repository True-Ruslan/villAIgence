package net.conczin.mca.livingworld.lore.editor;

import net.conczin.mca.network.Network;
import net.conczin.mca.network.s2c.OperatorLoreResponse;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/** Minecraft bridge that binds each Operator Lore request and response to one authenticated player. */
public final class OperatorLoreNetworkSession {
    private OperatorLoreNetworkSession() {
    }

    public static void handleRead(
            ServerPlayer player,
            int requestId,
            String scope,
            int villagerEntityId
    ) {
        dispatch(
                player,
                requestId,
                () -> OperatorLoreServerAuthority.read(player, scope, villagerEntityId)
        );
    }

    public static void handleWrite(
            ServerPlayer player,
            int requestId,
            String scope,
            int villagerEntityId,
            String expectedRevision,
            String value
    ) {
        dispatch(
                player,
                requestId,
                () -> OperatorLoreServerAuthority.write(
                        player,
                        scope,
                        villagerEntityId,
                        expectedRevision,
                        value
                )
        );
    }

    private static void dispatch(
            ServerPlayer player,
            int requestId,
            OperatorLoreNetworkSessionCore.AuthorityCall authority
    ) {
        Objects.requireNonNull(player, "player");
        OperatorLoreNetworkSessionCore.Session session = new OperatorLoreNetworkSessionCore.Session(
                player.getUUID(),
                envelope -> Network.sendToPlayer(
                        new OperatorLoreResponse(envelope.requestId(), envelope.result()),
                        player
                )
        );
        OperatorLoreNetworkSessionCore.execute(session, requestId, authority);
    }
}
