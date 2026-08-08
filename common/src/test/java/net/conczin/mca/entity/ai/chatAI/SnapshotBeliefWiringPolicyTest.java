package net.conczin.mca.entity.ai.chatAI;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotBeliefWiringPolicyTest {
    @Test
    void openAiSnapshotPathExposesCandidatesWithoutChangingLegacyStrategyContract() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"
        ));

        assertTrue(source.contains("record SnapshotAnswer("));
        assertTrue(source.contains("Optional<String> message"));
        assertTrue(source.contains("List<String> beliefCandidates"));
        assertTrue(source.contains("SnapshotAnswer answerDetailed("));
        assertTrue(source.contains("return answerDetailed(server, player, villager, msg, snapshot, deadline).message();"));
        assertTrue(source.contains("response.answer.beliefCandidates()"));
    }

    @Test
    void chatAiPersistsDialogueBeforeUsingItsExactSourceForPlayerToldBeliefs() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java"
        ));

        assertTrue(source.contains("openAIChatAI.answerDetailed"));
        assertTrue(source.contains("Optional<MemoryEvent> sourceEvent = rememberMemory2Dialogue"));
        assertTrue(source.contains("sourceEvent.ifPresent(source ->"));
        assertTrue(source.contains("PlayerToldBeliefLifecycle.recordCandidatesIfEnabled"));
        assertTrue(source.contains("config.memory2Enabled && config.semanticBeliefExtractionEnabled"));
        assertTrue(source.contains("snapshot.playerId()"));
        assertTrue(source.contains("snapshotAnswer.beliefCandidates()"));
    }

    @Test
    void classicAndInworldPathsAreNotWiredToCandidatePersistence() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java"
        ));

        assertTrue(source.contains("Optional<String> answer = strategy.answer(player, villager, msg);"));
        assertTrue(source.contains("return strategy.answer(player, villager, msg);"));
    }
}
