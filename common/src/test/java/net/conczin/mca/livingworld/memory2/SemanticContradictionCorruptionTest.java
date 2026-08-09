package net.conczin.mca.livingworld.memory2;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticContradictionCorruptionTest {
    private static final Gson GSON = new Gson();

    @TempDir
    Path tempDir;

    @Test
    void malformedDeserializedSnapshotFailsClosedWithoutThrowing() {
        MemoryEvent canonical = canonicalEvent();
        JsonObject json = GSON.toJsonTree(canonical).getAsJsonObject();
        json.getAsJsonObject("semanticContradiction")
                .getAsJsonObject("first")
                .remove("relatedEntities");
        MemoryEvent malformed = GSON.fromJson(json, MemoryEvent.class);

        boolean valid = assertDoesNotThrow(() -> SemanticContradictionPolicy.valid(malformed));

        assertFalse(valid);
    }

    @Test
    void malformedPersistedContradictionIsIgnoredByResolvedHistory() throws Exception {
        Path world = tempDir.resolve("malformed-world");
        Path livingWorld = world.resolve("livingworld");
        Files.createDirectories(livingWorld);

        MemoryEvent canonical = canonicalEvent();
        JsonObject malformed = GSON.toJsonTree(canonical).getAsJsonObject();
        malformed.getAsJsonObject("semanticContradiction")
                .getAsJsonObject("second")
                .remove("kind");

        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject eventsByNpc = new JsonObject();
        JsonArray events = new JsonArray();
        events.add(malformed);
        eventsByNpc.add(canonical.ownerNpcId().toString(), events);
        root.add("eventsByNpc", eventsByNpc);
        Files.writeString(livingWorld.resolve("memory2.json"), GSON.toJson(root));

        List<SemanticContradictionHistory.ResolvedSemanticContradiction> history = assertDoesNotThrow(
                () -> SemanticContradictionHistory.load(world, canonical.ownerNpcId(), id(90), 8)
        );

        assertTrue(history.isEmpty());
    }

    private static MemoryEvent canonicalEvent() {
        UUID npc = id(1);
        SemanticMemoryEntry first = new SemanticMemoryEntry(
                id(101),
                npc,
                SemanticMemoryEntry.Kind.FACT,
                "The gate is open",
                List.of(id(90)),
                MemoryEvent.Provenance.SYSTEM_OBSERVED,
                10L,
                0L,
                100,
                100,
                List.of(id(901))
        );
        SemanticMemoryEntry second = new SemanticMemoryEntry(
                id(102),
                npc,
                SemanticMemoryEntry.Kind.BELIEF,
                "The gate is closed",
                List.of(id(90)),
                MemoryEvent.Provenance.NPC_TOLD,
                11L,
                0L,
                60,
                60,
                List.of(id(902))
        );
        return SemanticContradictionAdapter.create(first, second, 200L).orElseThrow();
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
