package net.conczin.mca.livingworld.memory2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.conczin.mca.livingworld.persistence.GsonJsonStoreCodec;
import net.conczin.mca.livingworld.persistence.JsonStoreRecovery;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/** World-local bounded persistent store for Memory 2.0 events. */
public final class MemoryEventStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonStoreRecovery.Codec<MemoryFile> CODEC =
            new GsonJsonStoreCodec<>(GSON, MemoryFile.class);
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

        List<MemoryEvent> before = List.copyOf(events);
        events.add(event);
        long nowGameTime = events.stream()
                .mapToLong(MemoryEvent::gameTime)
                .max()
                .orElse(event.gameTime());
        List<MemoryEvent> retained = MemoryEventRetentionPolicy.selectRetained(
                events,
                safeMax,
                nowGameTime
        );

        events.clear();
        events.addAll(retained);
        if (!before.equals(retained)) save();
    }

    public synchronized List<MemoryEvent> getRecent(UUID npcId, int maxResults) {
        return getRecentMatching(npcId, maxResults, ignored -> true);
    }

    public synchronized Optional<MemoryEvent> findById(UUID npcId, UUID eventId) {
        if (npcId == null || eventId == null) return Optional.empty();
        List<MemoryEvent> events = data.eventsByNpc.get(npcId.toString());
        if (events == null || events.isEmpty()) return Optional.empty();
        return events.stream().filter(event -> eventId.equals(event.id())).findFirst();
    }

    synchronized List<MemoryEvent> getRecentMatching(
            UUID npcId,
            int maxResults,
            Predicate<MemoryEvent> predicate
    ) {
        if (npcId == null || maxResults <= 0 || predicate == null) return List.of();
        List<MemoryEvent> events = data.eventsByNpc.get(npcId.toString());
        if (events == null || events.isEmpty()) return List.of();
        return events.stream()
                .filter(predicate)
                .sorted(NEWEST_FIRST)
                .limit(maxResults)
                .toList();
    }

    private MemoryFile load() {
        MemoryFile loaded = JsonStoreRecovery.loadOrRecover(
                file,
                CODEC,
                value -> value != null
                        && value.version == FORMAT_VERSION
                        && value.eventsByNpc != null,
                MemoryFile::new
        );

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
    }

    private void save() {
        JsonStoreRecovery.writeAtomic(file, CODEC, data);
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
