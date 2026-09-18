package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    void boundedBatchReadReturnsRequestedDirectedStatesAndNeutralForMissingTargets() {
        Path world = tempDir.resolve("batch");
        UUID source = id(1);
        UUID positive = id(2);
        UUID adverse = id(3);
        UUID missing = id(4);

        NpcSocialGraphStore.forWorld(world).applyDelta(
                source,
                positive,
                new NpcSocialDelta(70, 80, 0, 70),
                100
        );
        NpcSocialGraphStore.forWorld(world).applyDelta(
                source,
                adverse,
                new NpcSocialDelta(-80, 0, 0, 0),
                100
        );
        NpcSocialGraphStore.forWorld(world).applyDelta(
                adverse,
                source,
                new NpcSocialDelta(90, 0, 0, 90),
                100
        );

        Map<UUID, NpcSocialState> states = NpcSocialGraphStrictPairReader.readMany(
                world,
                source,
                List.of(positive, adverse, missing)
        );

        assertEquals(new NpcSocialState(70, 80, 0, 70), states.get(positive));
        assertEquals(new NpcSocialState(-80, 0, 0, 0), states.get(adverse));
        assertEquals(NpcSocialState.NEUTRAL, states.get(missing));
        assertEquals(3, states.size());
    }

    @Test
    void boundedBatchReadRejectsMoreThanFourTargets() {
        Path world = tempDir.resolve("too-many");
        UUID source = id(10);

        assertThrows(IllegalArgumentException.class, () -> NpcSocialGraphStrictPairReader.readMany(
                world,
                source,
                List.of(id(11), id(12), id(13), id(14), id(15))
        ));
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
