package net.conczin.mca.livingworld.memory;

import net.minecraft.util.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistentChatMemoryWorkingMemoryTest {
    @TempDir
    Path tempDir;

    @Test
    void loadReturnsOnlyLatestTwelveMessagesWithCodePointBound() {
        UUID villager = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        ConversationMemoryStore store = ConversationMemoryStore.forWorld(tempDir);

        for (int i = 0; i < 7; i++) {
            String assistant = i == 6 ? "🙂".repeat(1300) : "a" + i;
            store.appendExchange(villager, player, "u" + i, assistant, 32, 5000);
        }

        List<Tuple<String, String>> messages = PersistentChatMemory.load(tempDir, villager, player);

        assertEquals(12, messages.size());
        assertEquals("u1", messages.getFirst().getB());
        String finalContent = messages.getLast().getB();
        assertEquals(1200, finalContent.codePointCount(0, finalContent.length()));
    }
}
