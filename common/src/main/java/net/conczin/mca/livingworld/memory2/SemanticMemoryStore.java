package net.conczin.mca.livingworld.memory2;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** World-local bounded persistent store for typed semantic Memory 2.0 entries. */
public final class SemanticMemoryStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ConcurrentMap<Path, SemanticMemoryStore> STORES = new ConcurrentHashMap<>();
    private static final Comparator<SemanticMemoryEntry> OLDEST_FIRST = Comparator
            .comparingLong(SemanticMemoryEntry::gameTime)
            .thenComparingLong(SemanticMemoryEntry::createdAtEpochMillis)
            .thenComparing(entry -> entry.id().toString());
    private static final Comparator<SemanticMemoryEntry> NEWEST_FIRST = OLDEST_FIRST.reversed();

    private final Path file;
    private SemanticMemoryFile data;

    public static SemanticMemoryStore forWorld(Path worldRoot) {
        Path file = worldRoot.toAbsolutePath().normalize().resolve("livingworld").resolve("semantic-memory.json");
        return STORES.computeIfAbsent(file, SemanticMemoryStore::new);
    }

    SemanticMemoryStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
        this.data = load();
    }

    public synchronized void append(SemanticMemoryEntry entry, int maxEntriesPerNpc) {
        if (entry == null) return;
        int safeMax = Math.max(1, maxEntriesPerNpc);
        String key = entry.ownerNpcId().toString();
        List<SemanticMemoryEntry> entries = data.entriesByNpc.computeIfAbsent(key, ignored -> new ArrayList<>());

        boolean duplicate = entries.stream().anyMatch(existing -> existing.id().equals(entry.id()));
        if (duplicate) return;

        entries.add(entry);
        entries.sort(OLDEST_FIRST);
        while (entries.size() > safeMax) entries.removeFirst();
        save();
    }

    public synchronized List<SemanticMemoryEntry> getRecent(UUID npcId, int maxResults) {
        if (npcId == null || maxResults <= 0) return List.of();
        List<SemanticMemoryEntry> entries = data.entriesByNpc.get(npcId.toString());
        if (entries == null || entries.isEmpty()) return List.of();
        return entries.stream()
                .sorted(NEWEST_FIRST)
                .limit(maxResults)
                .toList();
    }

    private SemanticMemoryFile load() {
        if (!Files.exists(file)) return new SemanticMemoryFile();
        try {
            SemanticMemoryFile loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), SemanticMemoryFile.class);
            if (loaded == null || loaded.version != FORMAT_VERSION) return new SemanticMemoryFile();
            if (loaded.entriesByNpc == null) loaded.entriesByNpc = new HashMap<>();

            Map<String, List<SemanticMemoryEntry>> sanitized = new HashMap<>();
            for (Map.Entry<String, List<SemanticMemoryEntry>> entry : loaded.entriesByNpc.entrySet()) {
                UUID owner = parseUuid(entry.getKey());
                if (owner == null || entry.getValue() == null) continue;
                List<SemanticMemoryEntry> entries = entry.getValue().stream()
                        .filter(value -> isValidForOwner(value, owner))
                        .sorted(OLDEST_FIRST)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                if (!entries.isEmpty()) sanitized.put(owner.toString(), entries);
            }
            loaded.entriesByNpc = sanitized;
            return loaded;
        } catch (IOException | RuntimeException ignored) {
            return new SemanticMemoryFile();
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
            throw new UncheckedIOException("Unable to persist VillAIgence semantic memory to " + file, e);
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isValidForOwner(SemanticMemoryEntry entry, UUID owner) {
        if (entry == null
                || entry.id() == null
                || !owner.equals(entry.ownerNpcId())
                || entry.kind() == null
                || entry.statement() == null
                || entry.statement().isBlank()
                || entry.provenance() == null) {
            return false;
        }
        return (entry.kind() == SemanticMemoryEntry.Kind.FACT
                && entry.provenance() == MemoryEvent.Provenance.SYSTEM_OBSERVED)
                || (entry.kind() == SemanticMemoryEntry.Kind.BELIEF
                && entry.provenance() != MemoryEvent.Provenance.SYSTEM_OBSERVED);
    }

    private static final class SemanticMemoryFile {
        int version = FORMAT_VERSION;
        Map<String, List<SemanticMemoryEntry>> entriesByNpc = new HashMap<>();
    }
}
