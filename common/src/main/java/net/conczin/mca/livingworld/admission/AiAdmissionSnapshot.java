package net.conczin.mca.livingworld.admission;

/** Immutable process-local admission/backpressure metrics. */
public record AiAdmissionSnapshot(
        AiAdmissionStageSnapshot chat,
        AiAdmissionStageSnapshot stt,
        AiAdmissionStageSnapshot tts
) {
}
