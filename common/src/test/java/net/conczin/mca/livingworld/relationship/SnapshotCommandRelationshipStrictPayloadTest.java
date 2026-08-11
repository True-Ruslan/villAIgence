package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SnapshotCommandRelationshipStrictPayloadTest {
    @TempDir
    Path tempDir;

    @Test
    void missingRequiredRelationshipDimensionFailsClosedWithoutMutation() throws IOException {
        Path world = tempDir.resolve("missing-fear");
        UUID villager = id(1);
        UUID player = id(2);
        Path file = relationshipFile(world);
        write(file, payload(villager, player, "{\"trust\":0,\"respect\":0,\"affinity\":0}"));
        byte[] before = Files.readAllBytes(file);

        assertFalse(SnapshotCommandRelationshipPolicy.isAllowed(
                true, world, villager, player, "follow-player"));
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    void outOfRangeRelationshipDimensionFailsClosedWithoutClampingAuthority() throws IOException {
        Path world = tempDir.resolve("out-of-range");
        UUID villager = id(10);
        UUID player = id(11);
        Path file = relationshipFile(world);
        write(file, payload(villager, player, "{\"trust\":0,\"respect\":0,\"fear\":-999,\"affinity\":0}"));
        byte[] before = Files.readAllBytes(file);

        assertFalse(SnapshotCommandRelationshipPolicy.isAllowed(
                true, world, villager, player, "follow-player"));
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    void nonCanonicalNumericRelationshipDimensionFailsClosed() throws IOException {
        Path world = tempDir.resolve("fractional");
        UUID villager = id(20);
        UUID player = id(21);
        Path file = relationshipFile(world);
        write(file, payload(villager, player, "{\"trust\":0,\"respect\":0,\"fear\":1.5,\"affinity\":0}"));

        assertFalse(SnapshotCommandRelationshipPolicy.isAllowed(
                true, world, villager, player, "follow-player"));
    }

    private static String payload(UUID villager, UUID player, String state) {
        return "{\"version\":1,\"relationships\":{\""
                + villager + "/" + player + "\":" + state + "}}";
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static Path relationshipFile(Path world) {
        return world.resolve("livingworld").resolve("relationships.json");
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
