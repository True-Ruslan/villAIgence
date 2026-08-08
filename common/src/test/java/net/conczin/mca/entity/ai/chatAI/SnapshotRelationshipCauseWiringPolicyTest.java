package net.conczin.mca.entity.ai.chatAI;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotRelationshipCauseWiringPolicyTest {
    @Test
    void snapshotAnswerCarriesOnlyServerCreatedRelationshipEventMetadata() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"
        ));

        assertTrue(source.contains("Optional<MemoryEvent> relationshipChangeEvent"));
        assertTrue(source.contains("Memory2RelationshipChangeIngestor.recordAndReturnIfEnabled"));
        assertTrue(source.contains("applySnapshotRelationshipDelta(snapshot, response.answer.relationshipDelta(), livingWorld)"));
        assertFalse(source.contains("relationshipReason"));
        assertFalse(source.contains("causeReason"));
        assertFalse(source.contains("relationshipChangeEventId"));
        assertFalse(source.contains("evidenceEventId"));
    }

    @Test
    void chatAiPersistsDialogueBeforeCauseAndCauseBeforeBeliefs() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java"
        ));

        int detailedAnswer = source.indexOf("openAIChatAI.answerDetailed");
        int dialogue = source.indexOf("Optional<MemoryEvent> sourceEvent = rememberMemory2Dialogue", detailedAnswer);
        int cause = source.indexOf("RelationshipCauseLifecycle.recordDialogueTurn", dialogue);
        int beliefs = source.indexOf("PlayerToldBeliefLifecycle.recordCandidatesIfEnabled", dialogue);

        assertTrue(detailedAnswer >= 0);
        assertTrue(dialogue > detailedAnswer);
        assertTrue(cause > dialogue);
        assertTrue(beliefs > cause);
        assertTrue(source.contains("snapshotAnswer.relationshipChangeEvent()"));
        assertTrue(source.contains("sourceEvent"));
    }

    @Test
    void classicAndInworldPathsRemainOutsideCausalPersistence() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java"
        ));

        int classicAnswer = source.indexOf("Optional<String> answer = strategy.answer(player, villager, msg);");
        int snapshotAnswer = source.indexOf("openAIChatAI.answerDetailed");
        int cause = source.indexOf("RelationshipCauseLifecycle.recordDialogueTurn");

        assertTrue(classicAnswer >= 0);
        assertTrue(snapshotAnswer > classicAnswer);
        assertTrue(cause > snapshotAnswer);
        assertTrue(source.contains("return strategy.answer(player, villager, msg);"));
    }
}
