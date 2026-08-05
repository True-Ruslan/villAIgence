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
    void transportCoreIsPackagePrivateAndInvokesProviderAndSinkExactlyOnce() throws IOException {
        String source = read(
                "common/src/main/java/net/conczin/mca/network/AuthenticatedTextTurn.java"
        );

        assertTrue(source.contains("new Session(player.getUUID(), villager.getUUID())"));
        assertTrue(source.contains("static Outcome execute("));
        assertFalse(source.contains("public static Outcome execute("));
        assertEquals(1, occurrences(source, "provider.answer(session, normalizedMessage)"));
        assertEquals(1, occurrences(source, "responseSink.deliver(session, response)"));
        assertTrue(source.contains("!value.isBlank() && !value.startsWith(\"/\")"));
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
