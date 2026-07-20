package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
