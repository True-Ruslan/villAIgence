package net.conczin.mca.livingworld.diagnostics;

/** Non-secret configuration metadata for one AI pipeline stage. */
public record AiStageConfig(
        AiConfigState state,
        boolean enabled,
        boolean credentialConfigured,
        String provider,
        String model,
        String endpointHost,
        String format
) {
    public AiStageConfig {
        state = state == null ? AiConfigState.MISCONFIGURED : state;
        provider = AiOperationStatus.sanitize(provider);
        model = AiOperationStatus.sanitize(model);
        endpointHost = AiOperationStatus.sanitize(endpointHost);
        format = AiOperationStatus.sanitize(format);
    }
}
