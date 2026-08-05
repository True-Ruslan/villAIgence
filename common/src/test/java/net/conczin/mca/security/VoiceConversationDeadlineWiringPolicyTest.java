package net.conczin.mca.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceConversationDeadlineWiringPolicyTest {
    @Test
    void productionVoiceTurnCreatesOneDeadlineBeforeQueueingAndSharesItAcrossAllProviderStages() throws IOException {
        Path root = Path.of("..").toAbsolutePath().normalize();
        String service = Files.readString(root.resolve(
                "fabric/src/main/java/net/conczin/mca/fabric/livingworld/voice/VoiceConversationService.java"
        ));

        assertEquals(1L, service.lines().filter(line -> line.contains("AiRequestDeadline.startTotalMillis(")).count());
        assertTrue(service.indexOf("AiRequestDeadline.startTotalMillis(")
                < service.indexOf("server.execute(() -> validateTargetAndTranscribe"));
        assertTrue(service.contains("microphonePcm, deadline"));
        assertTrue(service.contains("ChatAI.answer(server, player, villager, transcript, snapshot, deadline)"));
        assertTrue(service.contains(
                "audioProvider.synthesize(new TtsRequest(text, profile.voiceId(), style), deadline)"
        ));
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
        assertFalse(transport.contains("Memory2DialogueLifecycle"));
        assertFalse(transport.contains("LivingWorldRelationshipStore"));
        assertEquals(1L, openAi.lines()
                .filter(line -> line.contains("applySnapshotRelationshipDelta(snapshot,"))
                .count());

        String providerCall =
                "openAIChatAI.answer(server, player, villager, msg, snapshot, deadline)";
        int providerCallIndex = chatAi.indexOf(providerCall);
        int snapshotCommitIndex = providerCallIndex < 0
                ? -1
                : chatAi.indexOf("rememberMemory2Dialogue(", providerCallIndex);
        assertTrue(providerCallIndex >= 0, "Snapshot Chat path must delegate the shared deadline");
        assertTrue(snapshotCommitIndex > providerCallIndex,
                "Snapshot dialogue commit must remain after the final provider result");
    }
}
