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

/** World-local bounded persistent store for Memory 2.0 events. */
public final class MemoryEventStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ConcurrentMap<Path, MemoryEventStore> STORES = new ConcurrentHashMap<>();
    private static final Comparator<MemoryEvent> OLDEST_FIRST = Comparator
            .comparingLong(MemoryEvent::gameTime)
            .thenComparingLong(MemoryEvent::createdAtEpochMillis)
            .thenComparing(event -> event.id().toString());
    private static final Comparator<MemoryEvent> NEWEST_FIRST = OLDEST_FIRST.reversed();

    private final Path file;
    private MemoryFile data;

    public static MemoryEventStore forWorld(Path worldRoot) {
        Path file = worldRoot.toAbsolutePath().normalize().resolve("livingworld").resolve("memory2.json");
        return STORES.computeIfAbsent(file, MemoryEventStore::new);
    }

    MemoryEventStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
        this.data = load();
    }

    public synchronized void append(MemoryEvent event, int maxEventsPerNpc) {
        if (event == null) return;
        int safeMax = Math.max(1, maxEventsPerNpc);
        String key = event.ownerNpcId().toString();
        List<MemoryEvent> events = data.eventsByNpc.computeIfAbsent(key, ignored -> new ArrayList<>());

        boolean duplicate = events.stream().anyMatch(existing -> existing.id().equals(event.id()));
        if (duplicate) return;

        events.add(event);
        events.sort(OLDEST_FIRST);
        while (events.size() > safeMax) events.removeFirst();
        save();
    }

    public synchronized List<MemoryEvent> getRecent(UUID npcId, int maxResults) {
        if (npcId == null || maxResults <= 0) return List.of();
        List<MemoryEvent> events = data.eventsByNpc.get(npcId.toString());
        if (events == null || events.isEmpty()) return List.of();
        return events.stream()
                .sorted(NEWEST_FIRST)
                .limit(maxResults)
                .toList();
    }

    private MemoryFile load() {
        if (!Files.exists(file)) return new MemoryFile();
        try {
            MemoryFile loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), MemoryFile.class);
            if (loaded == null || loaded.version != FORMAT_VERSION) return new MemoryFile();
            if (loaded.eventsByNpc == null) loaded.eventsByNpc = new HashMap<>();

            Map<String, List<MemoryEvent>> sanitized = new HashMap<>();
            for (Map.Entry<String, List<MemoryEvent>> entry : loaded.eventsByNpc.entrySet()) {
                UUID owner = parseUuid(entry.getKey());
                if (owner == null || entry.getValue() == null) continue;
                List<MemoryEvent> events = entry.getValue().stream()
                        .filter(event -> isValidForOwner(event, owner))
                        .sorted(OLDEST_FIRST)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                if (!events.isEmpty()) sanitized.put(owner.toString(), events);
            }
            loaded.eventsByNpc = sanitized;
            return loaded;
        } catch (IOException | RuntimeException ignored) {
            return new MemoryFile();
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
            throw new UncheckedIOException("Unable to persist VillAIgence Memory 2.0 events to " + file, e);
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

    private static boolean isValidForOwner(MemoryEvent event, UUID owner) {
        return event != null
                && event.id() != null
                && owner.equals(event.ownerNpcId())
                && event.type() != null
                && event.summary() != null
                && !event.summary().isBlank()
                && event.provenance() != null;
    }

    private static final class MemoryFile {
        int version = FORMAT_VERSION;
        Map<String, List<MemoryEvent>> eventsByNpc = new HashMap<>();
    }
}
