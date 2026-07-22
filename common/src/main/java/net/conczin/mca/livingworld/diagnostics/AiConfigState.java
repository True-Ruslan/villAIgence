package net.conczin.mca.livingworld.diagnostics;

/** Safe operator-facing configuration readiness for an AI pipeline stage. */
public enum AiConfigState {
    CONFIGURED,
    DISABLED,
    MISCONFIGURED
}
