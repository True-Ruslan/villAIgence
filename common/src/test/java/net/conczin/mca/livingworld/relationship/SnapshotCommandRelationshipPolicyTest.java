package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotCommandRelationshipPolicyTest {
    @TempDir
    Path tempDir;

    @Test
    void disabledRelationshipStatePreservesLegacyCommandPermission() {
        assertTrue(SnapshotCommandRelationshipPolicy.isAllowed(
                false, null, null, null, "follow-player"));
    }

    @Test
    void missingRelationshipStoreIsNeutralAndReadOnlyForFollow() {
        Path world = tempDir.resolve("neutral");
        Path relationshipFile = relationshipFile(world);
        UUID villager = id(1);
        UUID player = id(2);

        assertFalse(Files.exists(relationshipFile));
        assertTrue(SnapshotCommandRelationshipPolicy.isAllowed(
                true, world, villager, player, "follow-player"));
        assertFalse(Files.exists(relationshipFile));
    }

    @Test
    void freshLowTrustOrHighFearBlocksFollowUsingExistingAuthorityPolicy() {
        Path lowTrustWorld = tempDir.resolve("low-trust");
        UUID villager = id(10);
        UUID player = id(11);
        LivingWorldRelationshipStore.forWorld(lowTrustWorld).applyDelta(
                villager, player, new LivingWorldRelationshipDelta(-26, 0, 0, 0), 100);
        assertFalse(SnapshotCommandRelationshipPolicy.isAllowed(
                true, lowTrustWorld, villager, player, "follow-player"));

        Path highFearWorld = tempDir.resolve("high-fear");
        LivingWorldRelationshipStore.forWorld(highFearWorld).applyDelta(
                villager, player, new LivingWorldRelationshipDelta(100, 0, 61, 0), 100);
        assertFalse(SnapshotCommandRelationshipPolicy.isAllowed(
                true, highFearWorld, villager, player, "follow-player"));
    }

    @Test
    void unrelatedSafeCommandsRemainIndependentOfRelationshipStore() throws IOException {
        Path world = tempDir.resolve("unrelated");
        Path relationshipFile = relationshipFile(world);
        Files.createDirectories(relationshipFile.getParent());
        Files.writeString(relationshipFile, "{ definitely-not-valid-json");

        assertTrue(SnapshotCommandRelationshipPolicy.isAllowed(
                true, world, id(20), id(21), "stay-here"));
        assertTrue(SnapshotCommandRelationshipPolicy.isAllowed(
                true, world, id(20), id(21), "open-trade-window"));
    }

    @Test
    void invalidRelationshipStoreFailsClosedForFollowWithoutRecoveryMutation() throws IOException {
        Path world = tempDir.resolve("invalid");
        Path relationshipFile = relationshipFile(world);
        Files.createDirectories(relationshipFile.getParent());
        Files.writeString(relationshipFile, "{ definitely-not-valid-json");
        byte[] before = Files.readAllBytes(relationshipFile);

        assertFalse(SnapshotCommandRelationshipPolicy.isAllowed(
                true, world, id(30), id(31), "follow-player"));

        assertArrayEquals(before, Files.readAllBytes(relationshipFile));
        assertFalse(Files.exists(relationshipFile.resolveSibling("relationships.json.corrupt")));
    }

    @Test
    void nonRegularRelationshipStoreFailsClosedForFollow() throws IOException {
        Path world = tempDir.resolve("non-regular");
        Path relationshipFile = relationshipFile(world);
        Files.createDirectories(relationshipFile);

        assertFalse(SnapshotCommandRelationshipPolicy.isAllowed(
                true, world, id(40), id(41), "follow-player"));
    }

    private static Path relationshipFile(Path world) {
        return world.resolve("livingworld").resolve("relationships.json");
    }

    private static UUID id(int value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", value));
    }
}
