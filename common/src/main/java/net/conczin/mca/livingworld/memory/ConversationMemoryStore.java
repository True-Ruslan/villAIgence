package net.conczin.mca.livingworld.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Small world-local persistent conversation store for LivingWorld's direct AI path.
 *
 * <p>The implementation deliberately stays dependency-free beyond Gson and hides the file format
 * behind this API so storage can later be migrated to SQLite or semantic memory.</p>
 */
public final class ConversationMemoryStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ConcurrentMap<Path, ConversationMemoryStore> STORES = new ConcurrentHashMap<>();

    private final Path file;
    private MemoryFile data;

    public static ConversationMemoryStore forWorld(Path worldRoot) {
        Path file = worldRoot.toAbsolutePath().normalize().resolve("livingworld").resolve("memory.json");
        return STORES.computeIfAbsent(file, ConversationMemoryStore::new);
    }

    ConversationMemoryStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
        this.data = load();
    }

    public synchronized List<MemoryMessage> getMessages(UUID villagerId, UUID playerId) {
        List<MemoryMessage> messages = data.conversations.get(key(villagerId, playerId));
        return messages == null ? List.of() : List.copyOf(messages);
    }

    public synchronized void appendExchange(
            UUID villagerId,
            UUID playerId,
            String userMessage,
            String assistantMessage,
            int maxMessages,
            int maxCharsPerMessage
    ) {
        int safeMaxMessages = Math.max(2, maxMessages);
        int safeMaxChars = Math.max(1, maxCharsPerMessage);
        List<MemoryMessage> messages = data.conversations.computeIfAbsent(
                key(villagerId, playerId), ignored -> new ArrayList<>()
        );
        messages.add(new MemoryMessage("user", trim(userMessage, safeMaxChars)));
        messages.add(new MemoryMessage("assistant", trim(assistantMessage, safeMaxChars)));
        while (messages.size() > safeMaxMessages) {
            messages.removeFirst();
        }
        save();
    }

    private MemoryFile load() {
        if (!Files.exists(file)) {
            return new MemoryFile();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            MemoryFile loaded = GSON.fromJson(json, MemoryFile.class);
            if (loaded == null || loaded.version != FORMAT_VERSION) {
                return new MemoryFile();
            }
            if (loaded.conversations == null) {
                loaded.conversations = new HashMap<>();
            }
            loaded.conversations.replaceAll((key, value) -> value == null ? new ArrayList<>() : new ArrayList<>(value));
            return loaded;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Unable to load LivingWorld memory from " + file, e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(
                    temp,
                    GSON.toJson(data),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to persist LivingWorld memory to " + file, e);
        }
    }

    private static String key(UUID villagerId, UUID playerId) {
        return villagerId + "/" + playerId;
    }

    private static String trim(String value, int maxChars) {
        String safe = value == null ? "" : value.strip();
        return safe.length() <= maxChars ? safe : safe.substring(0, maxChars);
    }

    private static final class MemoryFile {
        int version = FORMAT_VERSION;
        Map<String, List<MemoryMessage>> conversations = new HashMap<>();
    }
}
