package net.conczin.mca.acceptancefixture;

import com.google.gson.JsonObject;

/** Shared in-process handoff between the Simple Voice Chat plugin and Fabric evidence writer. */
final class ProductionAcceptanceVoiceTransportState {
    static final String SCENARIO = "VAI-AI-005";
    static final String PASS_MARKER = "VAI-AI-005-VOICE-TRANSPORT-PASS";

    private static volatile Report report;
    private static volatile Throwable failure;

    private ProductionAcceptanceVoiceTransportState() {
    }

    static synchronized void pass(Report value) {
        if (report != null || failure != null) {
            throw new IllegalStateException("voice transport acceptance completed more than once");
        }
        report = value;
    }

    static synchronized void fail(Throwable value) {
        if (report == null && failure == null) {
            failure = value;
        }
    }

    static Report report() {
        return report;
    }

    static Throwable failure() {
        return failure;
    }

    record Report(
            int sampleRate,
            int frameSamples,
            int encodedFrames,
            int encodedBytes,
            int acceptedPackets,
            int decodedFrames,
            int decodedSamples,
            int lostPackets,
            int plcSamples,
            boolean duplicateRejected,
            boolean outOfOrderRejected,
            boolean budgetExhaustionRejected,
            long pcmBudgetMaxBytes,
            long peakPcmBytes,
            long pcmBytesAfterCancel,
            long pcmBytesAfterDisconnect,
            boolean postCancelRejected,
            boolean postDisconnectRejected,
            boolean encoderClosed,
            boolean primaryDecoderClosed,
            boolean disconnectDecoderClosed
    ) {
        JsonObject toJson() {
            JsonObject root = new JsonObject();
            root.addProperty("schema", 1);
            root.addProperty("scenario", SCENARIO);
            root.addProperty("status", "PASS");
            root.addProperty("sampleRate", sampleRate);
            root.addProperty("frameSamples", frameSamples);
            root.addProperty("encodedFrames", encodedFrames);
            root.addProperty("encodedBytes", encodedBytes);
            root.addProperty("acceptedPackets", acceptedPackets);
            root.addProperty("decodedFrames", decodedFrames);
            root.addProperty("decodedSamples", decodedSamples);
            root.addProperty("lostPackets", lostPackets);
            root.addProperty("plcSamples", plcSamples);
            root.addProperty("duplicateRejected", duplicateRejected);
            root.addProperty("outOfOrderRejected", outOfOrderRejected);
            root.addProperty("budgetExhaustionRejected", budgetExhaustionRejected);
            root.addProperty("pcmBudgetMaxBytes", pcmBudgetMaxBytes);
            root.addProperty("peakPcmBytes", peakPcmBytes);
            root.addProperty("pcmBytesAfterCancel", pcmBytesAfterCancel);
            root.addProperty("pcmBytesAfterDisconnect", pcmBytesAfterDisconnect);
            root.addProperty("postCancelRejected", postCancelRejected);
            root.addProperty("postDisconnectRejected", postDisconnectRejected);
            root.addProperty("encoderClosed", encoderClosed);
            root.addProperty("primaryDecoderClosed", primaryDecoderClosed);
            root.addProperty("disconnectDecoderClosed", disconnectDecoderClosed);
            return root;
        }
    }
}
