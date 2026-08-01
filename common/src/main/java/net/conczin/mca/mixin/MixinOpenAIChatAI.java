package net.conczin.mca.mixin;

import net.conczin.mca.Config;
import net.conczin.mca.entity.ai.chatAI.OpenAIChatAI;
import net.conczin.mca.livingworld.context.LivingWorldContextSnapshot;
import net.conczin.mca.livingworld.context.SnapshotContextPromptPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the provider implementation untouched while inserting immutable server-authored lore into its prompt. */
@Mixin(value = OpenAIChatAI.class, remap = false)
abstract class MixinOpenAIChatAI {
    @Inject(method = "buildSnapshotSystem", at = @At("RETURN"), cancellable = true)
    private static void mca$insertOperatorAuthoredContext(
            Config config,
            boolean inHouse,
            LivingWorldContextSnapshot snapshot,
            CallbackInfoReturnable<String> callback
    ) {
        callback.setReturnValue(SnapshotContextPromptPolicy.insertOperatorLore(
                callback.getReturnValue(),
                snapshot.operatorAuthoredContext()
        ));
    }
}
