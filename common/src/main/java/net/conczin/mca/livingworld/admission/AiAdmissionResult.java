package net.conczin.mca.livingworld.admission;

import org.jetbrains.annotations.Nullable;

/** Non-blocking admission decision plus an owned capacity permit when allowed. */
public record AiAdmissionResult(
        AiAdmissionDecision decision,
        @Nullable AiAdmissionController.Permit permit
) {
    public boolean allowed() {
        return decision == AiAdmissionDecision.ALLOWED && permit != null;
    }
}
