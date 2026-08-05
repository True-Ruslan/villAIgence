package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceConversationDeadlineWiringPolicyTest {
    @Test
    void productionVoiceTurnCreatesOneDeadlineBeforeQueueingAndSharesItAcrossAllProviderStages() throws IOException {
        Path root = Path.of("..").toAbsolutePath().normalize();
        String service = Files.readString(root.resolve(
                "fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceConversationService.java"
        ));

        assertEquals(1L, service.lines().filter(line -> line.contains("AiRequestDeadline.startTotalMillis(")).count());
        assertTrue(service.indexOf("AiRequestDeadline.startTotalMillis(") < service.indexOf("server.execute(() -> validateTargetAndTranscribe"));
        assertTrue(service.contains("microphonePcm, deadline"));
        assertTrue(service.contains("ChatAI.answer(server, player, villager, transcript, snapshot, deadline)"));
        assertTrue(service.contains("audioProvider.synthesize(new TtsRequest(text, profile.voiceId(), style), deadline)"));
    }

    @Test
    void retryTransportDoesNotOwnDialogueOrRelationshipCommitSites() throws IOException {
        Path root = Path.of("..").toAbsolutePath().normalize();
        String transport = Files.readString(root.resolve(
                "common/src/main/java/net/conczin/mca/livingworld/ai/ChatCompletionHttpClient.java"
        ));
        String openAi = Files.readString(root.resolve(
                "common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java"
        ));
        String chatAi = Files.readString(root.resolve(
                "common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java"
        ));

        assertTrue(transport.contains("AiRequestDeadline deadline"));
        assertTrue(!transport.contains("Memory2DialogueLifecycle"));
        assertTrue(!transport.contains("LivingWorldRelationshipStore"));
        assertEquals(1L, openAi.lines().filter(line -> line.contains("applySnapshotRelationshipDelta(snapshot,")).count());
        assertEquals(1L, chatAi.lines().filter(line -> line.contains("rememberMemory2Dialogue(") && !line.contains("private static")).count());
    }
}
