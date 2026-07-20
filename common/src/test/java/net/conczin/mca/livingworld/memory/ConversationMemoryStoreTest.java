package net.conczin.mca.livingworld.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationMemoryStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void survivesStoreRecreationAndSeparatesPlayers() {
        UUID villager = UUID.randomUUID();
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        Path file = tempDir.resolve("memory.json");

        ConversationMemoryStore first = new ConversationMemoryStore(file);
        first.appendExchange(villager, playerA, "I am Ruslan", "I will remember you", 16, 1200);
        first.appendExchange(villager, playerB, "I am Alex", "Hello Alex", 16, 1200);

        ConversationMemoryStore reloaded = new ConversationMemoryStore(file);
        assertEquals(List.of(
                new MemoryMessage("user", "I am Ruslan"),
                new MemoryMessage("assistant", "I will remember you")
        ), reloaded.getMessages(villager, playerA));
        assertEquals(2, reloaded.getMessages(villager, playerB).size());
    }

    @Test
    void boundsHistoryAndMessageLength() {
        UUID villager = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        ConversationMemoryStore store = new ConversationMemoryStore(tempDir.resolve("memory.json"));

        store.appendExchange(villager, player, "123456789", "abcdefghi", 4, 5);
        store.appendExchange(villager, player, "second", "reply2", 4, 5);
        store.appendExchange(villager, player, "third", "reply3", 4, 5);

        assertEquals(List.of(
                new MemoryMessage("user", "third"),
                new MemoryMessage("assistant", "reply")
        ), store.getMessages(villager, player).subList(2, 4));
        assertEquals(4, store.getMessages(villager, player).size());
    }
}
