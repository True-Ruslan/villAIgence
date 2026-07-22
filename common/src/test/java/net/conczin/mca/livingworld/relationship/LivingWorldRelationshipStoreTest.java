package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingWorldRelationshipStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsAcrossReloadAndSeparatesNpcPlayerPairs() {
        UUID villager = UUID.randomUUID();
        UUID otherVillager = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        Path file = tempDir.resolve("relationships.json");

        LivingWorldRelationshipStore first = new LivingWorldRelationshipStore(file);
        first.applyDelta(villager, player, new LivingWorldRelationshipDelta(2, 1, -1, 2), 2);
        first.applyDelta(villager, otherPlayer, new LivingWorldRelationshipDelta(-2, 0, 2, -1), 2);
        first.applyDelta(otherVillager, player, new LivingWorldRelationshipDelta(1, 2, 0, 0), 2);

        LivingWorldRelationshipStore reloaded = new LivingWorldRelationshipStore(file);
        assertEquals(new LivingWorldRelationshipState(2, 1, -1, 2), reloaded.get(villager, player));
        assertEquals(new LivingWorldRelationshipState(-2, 0, 2, -1), reloaded.get(villager, otherPlayer));
        assertEquals(new LivingWorldRelationshipState(1, 2, 0, 0), reloaded.get(otherVillager, player));
        assertEquals(LivingWorldRelationshipState.NEUTRAL, reloaded.get(otherVillager, otherPlayer));
    }

    @Test
    void repeatedDeltasRemainBounded() {
        UUID villager = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        LivingWorldRelationshipStore store = new LivingWorldRelationshipStore(tempDir.resolve("relationships.json"));

        for (int i = 0; i < 100; i++) {
            store.applyDelta(villager, player, new LivingWorldRelationshipDelta(100, -100, 100, -100), 2);
        }

        assertEquals(new LivingWorldRelationshipState(100, -100, 100, -100), store.get(villager, player));
    }

    @Test
    void reportsExactPersistedBeforeAfterAndAppliedDelta() {
        UUID villager = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        LivingWorldRelationshipStore store = new LivingWorldRelationshipStore(tempDir.resolve("relationships.json"));
        store.applyDelta(villager, player, new LivingWorldRelationshipDelta(10, 4, 1, 7), 100);

        LivingWorldRelationshipChange change = store.applyDeltaWithResult(
                villager,
                player,
                new LivingWorldRelationshipDelta(2, -1, -1, 1),
                5
        );

        assertEquals(new LivingWorldRelationshipState(10, 4, 1, 7), change.before());
        assertEquals(new LivingWorldRelationshipState(12, 3, 0, 8), change.after());
        assertEquals(new LivingWorldRelationshipDelta(2, -1, -1, 1), change.appliedDelta());
        assertTrue(change.changed());
        assertEquals(change.after(), store.get(villager, player));
    }

    @Test
    void reportsActualAppliedDeltaAfterRelationshipBounds() {
        UUID villager = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        LivingWorldRelationshipStore store = new LivingWorldRelationshipStore(tempDir.resolve("relationships.json"));
        store.applyDelta(villager, player, new LivingWorldRelationshipDelta(99, -99, 0, 0), 100);

        LivingWorldRelationshipChange change = store.applyDeltaWithResult(
                villager,
                player,
                new LivingWorldRelationshipDelta(5, -5, 3, 0),
                5
        );

        assertEquals(new LivingWorldRelationshipState(99, -99, 0, 0), change.before());
        assertEquals(new LivingWorldRelationshipState(100, -100, 3, 0), change.after());
        assertEquals(new LivingWorldRelationshipDelta(1, -1, 3, 0), change.appliedDelta());
        assertTrue(change.changed());
    }

    @Test
    void saturatedNoOpReportsUnchangedAndPreservesCompatibilityMethod() {
        UUID villager = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        LivingWorldRelationshipStore store = new LivingWorldRelationshipStore(tempDir.resolve("relationships.json"));
        LivingWorldRelationshipState saturated = store.applyDelta(
                villager,
                player,
                new LivingWorldRelationshipDelta(100, -100, 0, 0),
                100
        );

        assertEquals(new LivingWorldRelationshipState(100, -100, 0, 0), saturated);

        LivingWorldRelationshipChange change = store.applyDeltaWithResult(
                villager,
                player,
                new LivingWorldRelationshipDelta(5, -5, 0, 0),
                5
        );

        assertEquals(saturated, change.before());
        assertEquals(saturated, change.after());
        assertEquals(LivingWorldRelationshipDelta.NONE, change.appliedDelta());
        assertFalse(change.changed());
    }

    @Test
    void corruptFileFailsOpenAndIsReplacedOnNextDelta() throws Exception {
        UUID villager = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        Path file = tempDir.resolve("relationships.json");
        Files.writeString(file, "{broken", StandardCharsets.UTF_8);

        LivingWorldRelationshipStore store = new LivingWorldRelationshipStore(file);
        assertEquals(LivingWorldRelationshipState.NEUTRAL, store.get(villager, player));

        store.applyDelta(villager, player, new LivingWorldRelationshipDelta(2, 1, -1, 2), 2);

        LivingWorldRelationshipStore reloaded = new LivingWorldRelationshipStore(file);
        assertEquals(new LivingWorldRelationshipState(2, 1, -1, 2), reloaded.get(villager, player));
    }
}
