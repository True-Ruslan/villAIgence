package net.conczin.mca.livingworld.knowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldEventStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsAcrossReloadAndKeepsOnlyNewestBoundedEvents() {
        Path file = tempDir.resolve("events.json");
        WorldEventStore first = new WorldEventStore(file);

        first.append(event("one", "minecraft:overworld", 0, 64, 0, 10), 3);
        first.append(event("two", "minecraft:overworld", 0, 64, 0, 20), 3);
        first.append(event("three", "minecraft:overworld", 0, 64, 0, 30), 3);
        first.append(event("four", "minecraft:overworld", 0, 64, 0, 40), 3);

        WorldEventStore reloaded = new WorldEventStore(file);
        List<WorldEvent> events = reloaded.queryRecent(
                "minecraft:overworld", 0, 64, 0,
                50, 1000, 128.0D, 10
        );

        assertEquals(List.of("four", "three", "two"), events.stream().map(WorldEvent::description).toList());
    }

    @Test
    void filtersByDimensionRadiusAgeAndMaximumCountNewestFirst() {
        WorldEventStore store = new WorldEventStore(tempDir.resolve("events.json"));
        store.append(event("old", "minecraft:overworld", 0, 64, 0, 10), 20);
        store.append(event("near-older", "minecraft:overworld", 3, 64, 4, 80), 20);
        store.append(event("near-newer", "minecraft:overworld", 2, 64, 0, 90), 20);
        store.append(event("too-far", "minecraft:overworld", 100, 64, 0, 95), 20);
        store.append(event("wrong-dimension", "minecraft:the_nether", 0, 64, 0, 99), 20);
        store.append(event("future", "minecraft:overworld", 0, 64, 0, 110), 20);

        List<WorldEvent> events = store.queryRecent(
                "minecraft:overworld", 0, 64, 0,
                100, 30, 16.0D, 2
        );

        assertEquals(List.of("near-newer", "near-older"), events.stream().map(WorldEvent::description).toList());
    }

    private static WorldEvent event(String description, String dimension, int x, int y, int z, long gameTime) {
        return new WorldEvent(
                UUID.nameUUIDFromBytes(description.getBytes()),
                WorldEvent.Type.NPC_ACTION,
                description,
                WorldEvent.Provenance.SYSTEM_OBSERVED,
                dimension,
                x,
                y,
                z,
                gameTime,
                UUID.nameUUIDFromBytes((description + "-actor").getBytes()),
                UUID.nameUUIDFromBytes((description + "-subject").getBytes())
        );
    }
}
