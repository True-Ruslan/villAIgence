package net.conczin.mca.livingworld.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationMemoryStoreRecoveryTest {
    @TempDir
    Path directory;

    @Test
    void corruptFileIsBackedUpAndStoreCanPersistAfterRecovery() throws IOException {
        Path file = directory.resolve("memory.json");
        byte[] corrupt = "{broken".getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);
        UUID villager = UUID.fromString("fa8dac15-0308-4899-b7d4-30096dc09666");
        UUID player = UUID.fromString("40829a06-3e67-499f-8ce9-6495314594b4");

        ConversationMemoryStore recovered = new ConversationMemoryStore(file);

        assertTrue(recovered.getMessages(villager, player).isEmpty());
        assertArrayEquals(corrupt, Files.readAllBytes(corruptBackup(file)));
        String canonical = Files.readString(file);
        assertTrue(canonical.contains("\"version\": 1"));
        assertTrue(canonical.contains("\"conversations\": {}"));

        recovered.appendExchange(
                villager,
                player,
                "hello",
                "response",
                8,
                64
        );
        byte[] backup = Files.readAllBytes(corruptBackup(file));
        ConversationMemoryStore restarted = new ConversationMemoryStore(file);

        assertEquals(2, restarted.getMessages(villager, player).size());
        assertArrayEquals(backup, Files.readAllBytes(corruptBackup(file)));
    }

    private static Path corruptBackup(Path file) {
        return file.resolveSibling(file.getFileName() + ".corrupt");
    }
}
