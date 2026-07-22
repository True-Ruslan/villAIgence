package net.conczin.mca.entity.ai.chatAI;

import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.livingworld.ai.AiProviderSettings;
import net.conczin.mca.livingworld.ai.LivingWorldAI;
import net.conczin.mca.livingworld.context.LivingWorldContextSnapshot;
import net.conczin.mca.livingworld.diagnostics.ChatDiagnosticsRecorder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.function.Supplier;

/** OpenAI-compatible chat strategy with process-local, content-safe diagnostics instrumentation. */
public final class DiagnosticsOpenAIChatAI extends OpenAIChatAI {
    @Override
    public Optional<String> answer(ServerPlayer player, VillagerEntityMCA villager, String msg) {
        return tracked(() -> super.answer(player, villager, msg));
    }

    @Override
    public Optional<String> answer(
            MinecraftServer server,
            ServerPlayer player,
            VillagerEntityMCA villager,
            String msg,
            LivingWorldContextSnapshot snapshot
    ) {
        return tracked(() -> super.answer(server, player, villager, msg, snapshot));
    }

    private Optional<String> tracked(Supplier<Optional<String>> operation) {
        AiProviderSettings provider = LivingWorldAI.resolveChatProviderSettings();
        ChatDiagnosticsRecorder.beginRequest();
        long startedNanos = System.nanoTime();
        boolean success = false;
        try {
            Optional<String> result = operation.get();
            success = result.isPresent() && !result.get().isBlank();
            return result;
        } finally {
            long durationMillis = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
            ChatDiagnosticsRecorder.finishRequest(
                    provider.endpoint(),
                    provider.model(),
                    durationMillis,
                    success
            );
        }
    }
}
