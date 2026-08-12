package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrictAuthorityCanonicalKeyTest {
    @TempDir
    Path tempDir;

    @Test
    void nonCanonicalRelationshipUuidKeyFailsClosedWithoutMutation() throws IOException {
        Path world = tempDir.resolve("relationship-short-uuid");
        String rawVillager = "1-1-1-1-1";
        String rawPlayer = "2-2-2-2-2";
        UUID villager = UUID.fromString(rawVillager);
        UUID player = UUID.fromString(rawPlayer);
        Path file = world.resolve("livingworld").resolve("relationships.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file,
                "{\"version\":1,\"relationships\":{\"" + rawVillager + "/" + rawPlayer
                        + "\":{\"trust\":0,\"respect\":0,\"fear\":0,\"affinity\":0}}}");
        byte[] before = Files.readAllBytes(file);

        assertFalse(SnapshotCommandRelationshipPolicy.isAllowed(
                true, world, villager, player, "follow-player"));
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test
    void nonCanonicalSocialGraphUuidKeyIsRejectedByStrictPairReader() throws IOException {
        Path world = tempDir.resolve("social-short-uuid");
        String rawSource = "3-3-3-3-3";
        String rawTarget = "4-4-4-4-4";
        UUID source = UUID.fromString(rawSource);
        UUID target = UUID.fromString(rawTarget);
        Path file = world.resolve("livingworld").resolve("npc-social-graph.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file,
                "{\"version\":1,\"edges\":{\"" + rawSource + "/" + rawTarget
                        + "\":{\"trust\":60,\"respect\":0,\"fear\":0,\"affinity\":60}},\"frontiers\":{}}");
        byte[] before = Files.readAllBytes(file);

        assertThrows(IllegalStateException.class,
                () -> NpcSocialGraphStrictPairReader.read(world, source, target));
        assertArrayEquals(before, Files.readAllBytes(file));
    }
}
