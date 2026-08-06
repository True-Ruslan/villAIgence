package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedTextTurnWiringPolicyTest {
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void chatPacketUsesAuthenticatedPlayerAndServerResolvedNpcBeforeDispatch() throws IOException {
        String mixin = read(
                "common/src/main/java/net/conczin/mca/mixin/MixinServerGamePacketListenerImpl.java"
        );

        assertTrue(mixin.contains("StringUtils.normalizeSpace(message.message())"));
        assertTrue(mixin.contains("ChatAI.getVillagerForConversation(player, msg)"));
        assertTrue(mixin.contains("AuthenticatedTextTurn.dispatch(player, resolved, msg)"));
        assertFalse(mixin.contains("message.player"));
        assertFalse(mixin.contains("message.villager"));
        assertFalse(mixin.contains("CompletableFuture.runAsync"));
        assertFalse(mixin.contains("conversationManager.addMessage"));
    }

    @Test
    void minecraftBridgeBindsTrustedIdentityAndDelegatesToPackagePrivateCore() throws IOException {
        String bridge = read(
                "common/src/main/java/net/conczin/mca/network/AuthenticatedTextTurn.java"
        );
        String core = read(
                "common/src/main/java/net/conczin/mca/network/AuthenticatedTextTurnCore.java"
        );

        assertTrue(bridge.contains("player.getUUID()"));
        assertTrue(bridge.contains("villager.getUUID()"));
        assertTrue(bridge.contains("AuthenticatedTextTurnCore.execute("));
        assertTrue(core.contains("final class AuthenticatedTextTurnCore"));
        assertFalse(core.contains("public final class AuthenticatedTextTurnCore"));
        assertFalse(core.contains("net.minecraft"));
        assertFalse(core.contains("VillagerEntityMCA"));
        assertFalse(core.contains("ServerPlayer"));
        assertEquals(1, occurrences(core, "provider.answer(session, normalizedMessage)"));
        assertEquals(1, occurrences(core, "responseSink.deliver(session, response)"));
        assertTrue(core.contains("!value.isBlank() && !value.startsWith(\"/\")"));
    }

    private static String read(String relativePath) throws IOException {
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
