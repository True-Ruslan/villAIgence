package net.conczin.mca.livingworld.context;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingWorldContextCapturePersonalitySocialWiringPolicyTest {
    @Test
    void playerVillagerSnapshotCapturesPersonalityOnceWithoutNpcCounterpart() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java"));
        String compact = source.replaceAll("\\s+", " ");

        assertTrue(compact.contains("PersonalityModule.applySnapshotBase(context, villager, player);"));
        assertFalse(compact.contains("PersonalityModule.apply(context, villager, player);"));
        assertTrue(compact.contains(
                "PersonalitySocialSnapshot personalitySocialSnapshot = PersonalitySocialSnapshotCapture.capture( worldRoot, villager, null );"
        ));
        assertTrue(compact.contains(
                "context, worldFacts, personalitySocialSnapshot, operatorAuthoredContext, memoryContext, semanticMemoryContext, contradictionContext, actions"
        ));
    }

    @Test
    void playerVillagerSnapshotDoesNotIntroduceGraphMutationOrProviderWork() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java"));

        assertFalse(source.contains("NpcSocialGraphStore"));
        assertFalse(source.contains("applyDelta("));
        assertFalse(source.contains("applyCausalDelta("));
        assertFalse(source.contains("ChatCompletion"));
        assertFalse(source.contains("LivingWorldAI"));
    }
}
