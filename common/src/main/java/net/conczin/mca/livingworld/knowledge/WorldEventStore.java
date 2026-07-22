package net.conczin.mca.livingworld.knowledge;

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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** World-local bounded factual event journal. */
public final class WorldEventStore {
    private static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ConcurrentMap<Path, WorldEventStore> STORES = new ConcurrentHashMap<>();

    private final Path file;
    private EventFile data;

    public static WorldEventStore forWorld(Path worldRoot) {
        Path file = worldRoot.toAbsolutePath().normalize().resolve("livingworld").resolve("events.json");
        return STORES.computeIfAbsent(file, WorldEventStore::new);
    }

    WorldEventStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
        this.data = load();
    }

    public synchronized void append(WorldEvent event, int maxEvents) {
        if (event == null) return;
        int safeMax = Math.max(1, maxEvents);
        data.events.add(event);
        while (data.events.size() > safeMax) data.events.removeFirst();
        save();
    }

    public synchronized List<WorldEvent> queryRecent(
            String dimension,
            int x,
            int y,
            int z,
            long now,
            long maxAgeTicks,
            double radius,
            int maxResults
    ) {
        if (dimension == null || dimension.isBlank() || maxResults <= 0 || radius < 0.0D || maxAgeTicks < 0L) return List.of();
        double radiusSquared = radius * radius;

        // Expired events should no longer consume the bounded in-memory journal. Persistence is updated on the
        // next append so a read-only context lookup never turns into a synchronous disk write.
        data.events.removeIf(event -> !isValid(event)
                || (event.gameTime() <= now && now - event.gameTime() > maxAgeTicks));

        return data.events.stream()
                .filter(event -> dimension.equals(event.dimension()))
                .filter(event -> event.gameTime() <= now)
                .filter(event -> distanceSquared(event, x, y, z) <= radiusSquared)
                .sorted(Comparator.comparingLong(WorldEvent::gameTime).reversed().thenComparing(event -> event.id().toString()))
                .limit(maxResults)
                .toList();
    }

    private EventFile load() {
        if (!Files.exists(file)) return new EventFile();
        try {
            EventFile loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), EventFile.class);
            if (loaded == null || loaded.version != FORMAT_VERSION) return new EventFile();
            if (loaded.events == null) loaded.events = new ArrayList<>();
            loaded.events.removeIf(event -> !isValid(event));
            return loaded;
        } catch (IOException | RuntimeException e) {
            return new EventFile();
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
            throw new UncheckedIOException("Unable to persist LivingWorld events to " + file, e);
        }
    }

    private static boolean isValid(WorldEvent event) {
        return event != null
                && event.id() != null
                && event.type() != null
                && event.provenance() != null
                && event.description() != null
                && !event.description().isBlank()
                && event.dimension() != null
                && !event.dimension().isBlank();
    }

    private static double distanceSquared(WorldEvent event, int x, int y, int z) {
        double dx = event.x() - x;
        double dy = event.y() - y;
        double dz = event.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class EventFile {
        int version = FORMAT_VERSION;
        List<WorldEvent> events = new ArrayList<>();
    }
}
