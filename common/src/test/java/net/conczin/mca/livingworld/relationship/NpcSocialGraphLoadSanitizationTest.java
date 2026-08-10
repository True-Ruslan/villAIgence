package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcSocialGraphLoadSanitizationTest {
    @TempDir
    Path tempDir;

    @Test
    void loadDropsMalformedSelfAndNeutralEdgesAndClampsValidState() throws Exception {
        UUID source = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID target = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID neutralTarget = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Path file = tempDir.resolve("npc-social-graph.json");

        Files.writeString(file, """
                {
                  "version": 1,
                  "edges": {
                    "%s/%s": {"trust": 500, "respect": -500, "fear": 7, "affinity": -7},
                    "%s/%s": {"trust": 9, "respect": 9, "fear": 9, "affinity": 9},
                    "%s/%s": {"trust": 0, "respect": 0, "fear": 0, "affinity": 0},
                    "%s/not-a-uuid": {"trust": 8, "respect": 8, "fear": 8, "affinity": 8},
                    "not-a-pair": {"trust": 8, "respect": 8, "fear": 8, "affinity": 8},
                    "%s/%s": null
                  }
                }
                """.formatted(
                source, target,
                source, source,
                source, neutralTarget,
                source,
                UUID.randomUUID(), UUID.randomUUID()
        ));

        NpcSocialGraphStore store = new NpcSocialGraphStore(file);

        assertEquals(new NpcSocialState(100, -100, 7, -7), store.get(source, target));
        assertEquals(NpcSocialState.NEUTRAL, store.get(source, source));
        assertEquals(NpcSocialState.NEUTRAL, store.get(source, neutralTarget));
    }

    @Test
    void duplicateLogicalPairWithDifferentUuidCasingFailsClosed() throws Exception {
        UUID source = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID target = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        String canonical = source + "/" + target;
        String alternate = canonical.toUpperCase(Locale.ROOT);
        Path file = tempDir.resolve("npc-social-graph.json");

        Files.writeString(file, """
                {
                  "version": 1,
                  "edges": {
                    "%s": {"trust": 20, "respect": 0, "fear": 0, "affinity": 0},
                    "%s": {"trust": 40, "respect": 0, "fear": 0, "affinity": 0}
                  }
                }
                """.formatted(canonical, alternate));

        assertEquals(NpcSocialState.NEUTRAL, new NpcSocialGraphStore(file).get(source, target));
    }

    @Test
    void invalidPersistedEntriesConsumeZeroOutgoingCapacity() throws Exception {
        UUID source = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        Path file = tempDir.resolve("npc-social-graph.json");
        StringBuilder edges = new StringBuilder();
        for (int index = 0; index < 62; index++) {
            appendEdge(edges, source + "/" + new UUID(400L + index, 4000L + index), 1);
        }
        appendEdge(edges, source + "/" + source, 99);
        appendEdge(edges, source + "/not-a-uuid", 99);
        appendEdge(edges, source + "/" + new UUID(900L, 9000L), 0);

        Files.writeString(file, "{\n  \"version\": 1,\n  \"edges\": {\n" + edges + "\n  }\n}\n");

        NpcSocialGraphStore store = new NpcSocialGraphStore(file);
        UUID admitted63 = new UUID(901L, 9001L);
        UUID admitted64 = new UUID(902L, 9002L);
        UUID rejected65 = new UUID(903L, 9003L);

        assertEquals(
                NpcSocialGraphMutation.Status.APPLIED,
                store.applyDelta(source, admitted63, new NpcSocialDelta(1, 0, 0, 0), 10).status()
        );
        assertEquals(
                NpcSocialGraphMutation.Status.APPLIED,
                store.applyDelta(source, admitted64, new NpcSocialDelta(1, 0, 0, 0), 10).status()
        );
        assertEquals(
                NpcSocialGraphMutation.Status.CAPACITY_REACHED,
                store.applyDelta(source, rejected65, new NpcSocialDelta(1, 0, 0, 0), 10).status()
        );
    }

    private static void appendEdge(StringBuilder edges, String key, int trust) {
        if (!edges.isEmpty()) edges.append(",\n");
        edges.append("    ")
                .append('"').append(key).append('"')
                .append(": {\"trust\": ").append(trust)
                .append(", \"respect\": 0, \"fear\": 0, \"affinity\": 0}");
    }
}
