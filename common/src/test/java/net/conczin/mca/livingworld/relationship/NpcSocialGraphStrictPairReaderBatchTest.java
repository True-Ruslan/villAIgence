package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NpcSocialGraphStrictPairReaderBatchTest {
    @TempDir
    Path tempDir;

    @Test
    void readManyReturnsExactRequestedStatesAndNeutralForMissingTargets() {
        Path world = tempDir.resolve("batch");
        UUID source = id(1);
        UUID positive = id(2);
        UUID missing = id(3);

        NpcSocialGraphStore.forWorld(world).applyDelta(
                source,
                positive,
                new NpcSocialDelta(40, 80, 0, 20),
                100
        );

        Map<UUID, NpcSocialState> states = NpcSocialGraphStrictPairReader.readMany(
                world,
                source,
                List.of(positive, missing)
        );

        assertEquals(2, states.size());
        assertEquals(new NpcSocialState(40, 80, 0, 20), states.get(positive));
        assertEquals(NpcSocialState.NEUTRAL, states.get(missing));
    }

    @Test
    void readManyPreservesStrictWholeFileValidation() throws IOException {
        Path world = tempDir.resolve("malformed-unrelated");
        UUID source = id(10);
        UUID requested = id(11);
        Path file = world.resolve("livingworld").resolve("npc-social-graph.json");
        Files.createDirectories(file.getParent());
        Files.writeString(
                file,
                """
                {
                  "version": 1,
                  "edges": {
                    "00000000-0000-0000-0000-000000000010/00000000-0000-0000-0000-000000000011": {
                      "trust": 40,
                      "respect": 80,
                      "fear": 0,
                      "affinity": 20
                    },
                    "not-a-canonical-edge": {
                      "trust": 1,
                      "respect": 1,
                      "fear": 1,
                      "affinity": 1
                    }
                  }
                }
                """
        );

        assertThrows(
                IllegalStateException.class,
                () -> NpcSocialGraphStrictPairReader.readMany(
                        world,
                        source,
                        List.of(requested)
                )
        );
    }

    @Test
    void readManyRejectsSelfNullAndDuplicateTargets() {
        Path world = tempDir.resolve("invalid-targets");
        UUID source = id(20);
        UUID target = id(21);

        assertThrows(
                IllegalArgumentException.class,
                () -> NpcSocialGraphStrictPairReader.readMany(
                        world,
                        source,
                        List.of(source)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NpcSocialGraphStrictPairReader.readMany(
                        world,
                        source,
                        java.util.Arrays.asList(target, null)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> NpcSocialGraphStrictPairReader.readMany(
                        world,
                        source,
                        List.of(target, target)
                )
        );
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
