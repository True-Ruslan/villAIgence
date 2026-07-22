package net.conczin.mca.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.admission.AiAdmissionController;
import net.conczin.mca.livingworld.admission.AiAdmissionSettings;
import net.conczin.mca.livingworld.diagnostics.AiDiagnostics;
import net.conczin.mca.livingworld.diagnostics.AiDiagnosticsConfigSnapshot;
import net.conczin.mca.livingworld.diagnostics.AiStatusReport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Read-only operator diagnostics for the public VillAIgence AI surface. */
public final class VillAIgenceCommand {
    private VillAIgenceCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villaigence")
                .requires(source -> source.hasPermission(2) || source.getServer().isSingleplayer())
                .then(Commands.literal("ai")
                        .then(Commands.literal("status")
                                .executes(VillAIgenceCommand::status))));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        LivingWorldConfig livingWorld = LivingWorldConfig.getInstance();
        AiDiagnosticsConfigSnapshot config = AiDiagnosticsConfigSnapshot.from(livingWorld);
        AiAdmissionSettings settings = AiAdmissionSettings.from(livingWorld);
        for (String line : AiStatusReport.format(config, AiDiagnostics.snapshot(), AiAdmissionController.snapshot(settings))) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }
}
