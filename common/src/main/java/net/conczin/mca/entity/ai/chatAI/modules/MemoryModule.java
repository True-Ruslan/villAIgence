package net.conczin.mca.entity.ai.chatAI.modules;

import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.memory2.Memory2ContextProvider;
import net.conczin.mca.livingworld.memory2.SemanticMemoryContextProvider;
import net.conczin.mca.livingworld.memory2.WorkingMemoryContext;
import net.conczin.mca.livingworld.memory2.WorkingMemoryOrchestrator;
import net.conczin.mca.livingworld.memory2.WorkingMemoryPromptFormatter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.List;

/** Shared fail-soft layered-memory prompt module used by both OpenAI dialogue routes. */
public final class MemoryModule {
    private MemoryModule() {
    }

    public static void apply(List<String> input, VillagerEntityMCA villager, ServerPlayer player) {
        if (input == null || villager == null || player == null) return;
        LivingWorldConfig config = LivingWorldConfig.getInstance();
        if (!config.memory2Enabled) return;

        try {
            Path worldRoot = player.serverLevel().getServer().getWorldPath(LevelResource.ROOT);
            long gameTime = villager.level().getGameTime();
            List<String> episodic = Memory2ContextProvider.load(
                    worldRoot,
                    villager.getUUID(),
                    player.getUUID(),
                    gameTime
            );
            List<String> semantic = SemanticMemoryContextProvider.load(
                    worldRoot,
                    villager.getUUID(),
                    player.getUUID(),
                    gameTime
            );
            WorkingMemoryContext workingMemory = WorkingMemoryOrchestrator.compose(List.of(), episodic, semantic);
            String section = WorkingMemoryPromptFormatter.promptSection(workingMemory);
            if (!section.isEmpty()) input.add(section);
        } catch (RuntimeException e) {
            MCA.LOGGER.warn(
                    "Unable to load layered Working Memory context for villager {} and player {}",
                    villager.getUUID(),
                    player.getUUID(),
                    e
            );
        }
    }
}
