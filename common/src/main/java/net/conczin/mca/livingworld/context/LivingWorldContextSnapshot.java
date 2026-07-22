package net.conczin.mca.livingworld.context;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Immutable data captured from Minecraft state before asynchronous LivingWorld AI processing. */
public record LivingWorldContextSnapshot(
        UUID playerId,
        UUID villagerId,
        String playerName,
        String villagerName,
        List<String> contextLines,
        List<String> worldFacts,
        List<String> memoryContext,
        List<ActionDescriptor> availableActions,
        long worldSeed,
        long gameTime,
        Path worldRoot,
        boolean child,
        boolean relative,
        String language
) {
    public LivingWorldContextSnapshot {
        contextLines = List.copyOf(contextLines);
        worldFacts = List.copyOf(worldFacts);
        memoryContext = List.copyOf(memoryContext);
        availableActions = List.copyOf(availableActions);
        worldRoot = worldRoot.toAbsolutePath().normalize();
        language = language == null ? "" : language;
    }

    public record ActionDescriptor(String command, String description) {
    }
}
