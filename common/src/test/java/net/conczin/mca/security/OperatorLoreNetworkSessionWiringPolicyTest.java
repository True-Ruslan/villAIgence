package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorLoreNetworkSessionWiringPolicyTest {
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void packetsDelegateAuthenticatedPlayerToOneSessionBridge() throws IOException {
        String read = source(
                "common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreReadRequest.java"
        );
        String write = source(
                "common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreWriteRequest.java"
        );
        String packets = read + write;

        assertTrue(read.contains("OperatorLoreNetworkSession.handleRead("));
        assertTrue(write.contains("OperatorLoreNetworkSession.handleWrite("));
        assertTrue(read.contains("player,"));
        assertTrue(write.contains("player,"));
        assertFalse(packets.contains("Network.sendToPlayer"));
        assertFalse(packets.contains("OperatorLoreServerAuthority"));
        assertFalse(packets.contains("playerId"));
        assertFalse(packets.contains("UUID"));
    }

    @Test
    void bridgeBindsEnvelopeAndResponseToTheSameServerPlayer() throws IOException {
        String bridge = source(
                "common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreNetworkSession.java"
        );

        assertTrue(bridge.contains("player.getUUID()"));
        assertTrue(bridge.contains("OperatorLoreServerAuthority.read(player,"));
        assertTrue(bridge.contains("OperatorLoreServerAuthority.write("));
        assertTrue(bridge.contains("new OperatorLoreResponse(envelope.requestId(), envelope.result())"));
        assertTrue(bridge.contains("Network.sendToPlayer("));
        assertTrue(bridge.contains("OperatorLoreNetworkSessionCore.execute(session, requestId, authority)"));
    }

    @Test
    void sessionCoreIsLoaderIndependentAndExactlyOnce() throws IOException {
        String core = source(
                "common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreNetworkSessionCore.java"
        );

        assertTrue(core.contains("final class OperatorLoreNetworkSessionCore"));
        assertFalse(core.contains("public final class OperatorLoreNetworkSessionCore"));
        assertFalse(core.contains("net.minecraft"));
        assertFalse(core.contains("net.conczin.mca.network"));
        assertEquals(1, occurrences(core, "authority.invoke()"));
        assertEquals(1, occurrences(core, "session.responseSink().deliver(envelope)"));
        assertTrue(core.contains("session.authenticatedPlayerId()"));
        assertTrue(core.contains("OperatorLoreProtocolPolicy.echo(requestId)"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
