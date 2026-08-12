package net.conczin.mca.entity.ai.chatAI;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotCommandRelationshipWiringPolicyTest {
    @Test
    void capturedAvailabilityIsRevalidatedAgainstFreshRelationshipStateBeforeExecution() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"));
        String compact = source.replaceAll("\\s+", " ");

        int captureGate = compact.indexOf(
                "boolean wasAllowed = snapshot.availableActions().stream().anyMatch(action -> action.command().equals(commandName));");
        int execute = compact.indexOf("server.execute(() -> {", captureGate);
        int freshGate = compact.indexOf(
                "SnapshotCommandRelationshipPolicy.isAllowed( relationshipStateEnabled, snapshot.worldRoot(), snapshot.villagerId(), snapshot.playerId(), commandName )",
                execute);
        int commandLookup = compact.indexOf("TriggerCommandInfos.findCommand(commandName, player, villager)", execute);

        assertTrue(captureGate >= 0);
        assertTrue(execute > captureGate);
        assertTrue(freshGate > execute);
        assertTrue(commandLookup > freshGate);
    }
}
