package net.conczin.mca.livingworld.actions;

/** Selects whether the AI may see the hard-coded MCA safe action whitelist. */
public final class LivingWorldActionPolicy {
    private LivingWorldActionPolicy() {
    }

    public static boolean shouldExposeTools(boolean livingWorldConfigured, boolean livingWorldSafeActionsEnabled, boolean legacyMcaToolsEnabled) {
        return livingWorldConfigured ? livingWorldSafeActionsEnabled : legacyMcaToolsEnabled;
    }
}
