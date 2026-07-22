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
        List<String> semanticMemoryContext,
        List<ActionDescriptor> availableActions,
        long worldSeed,
        long gameTime,
        Path worldRoot,
        boolean child,
        boolean relative,
        String language
) {
    public LivingWorldContextSnapshot {
        contextLines = contextLines == null ? List.of() : List.copyOf(contextLines);
        worldFacts = worldFacts == null ? List.of() : List.copyOf(worldFacts);
        memoryContext = memoryContext == null ? List.of() : List.copyOf(memoryContext);
        semanticMemoryContext = semanticMemoryContext == null ? List.of() : List.copyOf(semanticMemoryContext);
        availableActions = availableActions == null ? List.of() : List.copyOf(availableActions);
        worldRoot = worldRoot.toAbsolutePath().normalize();
        language = language == null ? "" : language;
    }

    /** Source-compatible constructor for call sites that know episodic Memory 2.0 but predate semantic memory. */
    public LivingWorldContextSnapshot(
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
        this(
                playerId,
                villagerId,
                playerName,
                villagerName,
                contextLines,
                worldFacts,
                memoryContext,
                List.of(),
                availableActions,
                worldSeed,
                gameTime,
                worldRoot,
                child,
                relative,
                language
        );
    }

    /** Source-compatible constructor for existing call sites that predate separate Memory 2.0 context. */
    public LivingWorldContextSnapshot(
            UUID playerId,
            UUID villagerId,
            String playerName,
            String villagerName,
            List<String> contextLines,
            List<String> worldFacts,
            List<ActionDescriptor> availableActions,
            long worldSeed,
            long gameTime,
            Path worldRoot,
            boolean child,
            boolean relative,
            String language
    ) {
        this(
                playerId,
                villagerId,
                playerName,
                villagerName,
                contextLines,
                worldFacts,
                List.of(),
                List.of(),
                availableActions,
                worldSeed,
                gameTime,
                worldRoot,
                child,
                relative,
                language
        );
    }

    public record ActionDescriptor(String command, String description) {
    }
}
