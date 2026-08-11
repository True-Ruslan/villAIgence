package net.conczin.mca.livingworld.context;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingWorldContextCaptureRelationshipAuthorizationWiringPolicyTest {
    @Test
    void captureUsesStrictRelationshipAuthorityWithoutRecoveryBeforeActionFiltering() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java"));
        String compact = source.replaceAll("\\s+", " ");

        String strictStateRead = "LivingWorldRelationshipStore.readStrict( worldRoot, villager.getUUID(), player.getUUID() )";
        String strictActionGate = "SnapshotCommandRelationshipPolicy.isAllowed( livingWorld.relationshipStateEnabled, worldRoot, villager.getUUID(), player.getUUID(), command.command )";

        assertTrue(compact.contains(strictStateRead));
        assertTrue(compact.contains(strictActionGate));
        assertFalse(source.contains("LivingWorldRelationshipStore.forWorld(worldRoot)"));
        assertFalse(source.contains("LivingWorldRelationshipActionPolicy.isAllowed(command.command, relationshipState)"));
    }
}
