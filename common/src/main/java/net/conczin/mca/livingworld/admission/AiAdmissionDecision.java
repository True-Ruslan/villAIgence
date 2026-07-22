package net.conczin.mca.livingworld.admission;

/** Result of non-blocking admission at an external AI provider boundary. */
public enum AiAdmissionDecision {
    ALLOWED,
    PLAYER_COOLDOWN,
    SATURATED,
    PROVIDER_COOLDOWN
}
