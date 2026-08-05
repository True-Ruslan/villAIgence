package net.conczin.mca.acceptancefixture;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.conczin.mca.livingworld.voice.VoicePcmBudget;

import java.util.ArrayList;
import java.util.List;

/** Test-only real Opus codec and bounded loopback transport acceptance plugin. */
public final class ProductionAcceptanceVoiceTransportPlugin implements VoicechatPlugin {
    private static final int SAMPLE_RATE = 48_000;
    private static final int FRAME_SAMPLES = 960;
    private static final int ENCODED_FRAMES = 4;
    private static final long PCM_BUDGET_BYTES = (long) FRAME_SAMPLES * ENCODED_FRAMES * 2L;

    private volatile VoicechatApi api;

    @Override
    public String getPluginId() {
        return "mca_production_acceptance_voice_transport";
    }

    @Override
    public void initialize(VoicechatApi api) {
        this.api = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, ignored -> execute());
    }

    private void execute() {
        if ("recovery".equals(System.getProperty("villaigence.acceptance.mode"))) {
            return;
        }
        try {
            ProductionAcceptanceVoiceTransportState.pass(runMatrix(requireApi()));
        } catch (Throwable failure) {
            ProductionAcceptanceVoiceTransportState.fail(failure);
        }
    }

    private VoicechatApi requireApi() {
        VoicechatApi value = api;
        if (value == null) {
            throw new IllegalStateException("Simple Voice Chat did not initialize the fixture plugin");
        }
        return value;
    }

    private static ProductionAcceptanceVoiceTransportState.Report runMatrix(VoicechatApi api) {
        OpusEncoder encoder = requireEncoder(api.createEncoder());
        OpusDecoder primaryDecoder = requireDecoder(api.createDecoder(), "primary");
        OpusDecoder disconnectDecoder = requireDecoder(api.createDecoder(), "disconnect");

        int encodedBytes = 0;
        int acceptedPackets;
        int decodedFrames;
        int decodedSamples;
        int lostPackets;
        int plcSamples;
        boolean duplicateRejected;
        boolean outOfOrderRejected;
        boolean budgetExhaustionRejected;
        long peakPcmBytes;
        long pcmBytesAfterCancel;
        long pcmBytesAfterDisconnect;
        boolean postCancelRejected;
        boolean postDisconnectRejected;
        try {
            List<byte[]> packets = new ArrayList<>(ENCODED_FRAMES);
            for (int frame = 0; frame < ENCODED_FRAMES; frame++) {
                byte[] packet = encoder.encode(frame(frame));
                if (packet == null || packet.length == 0 || packet.length > 1_275) {
                    throw new IllegalStateException(
                            "real Opus encoder returned an invalid packet for frame " + frame
                    );
                }
                packets.add(packet);
                encodedBytes += packet.length;
            }

            VoicePcmBudget primaryBudget = new VoicePcmBudget(PCM_BUDGET_BYTES);
            LoopbackSession primary = new LoopbackSession(primaryDecoder, primaryBudget);
            requireAccepted(primary.accept(0L, packets.get(0)), "sequence 0");
            requireAccepted(primary.accept(1L, packets.get(1)), "sequence 1");
            requireAccepted(primary.accept(3L, packets.get(3)), "sequence 3 after one loss");

            duplicateRejected = !primary.accept(3L, packets.get(3));
            outOfOrderRejected = !primary.accept(2L, packets.get(2));
            budgetExhaustionRejected = !primaryBudget.tryReserve(2L);
            acceptedPackets = primary.acceptedPackets;
            decodedFrames = primary.decodedFrames;
            decodedSamples = primary.decodedSamples;
            lostPackets = primary.lostPackets;
            plcSamples = primary.plcSamples;
            peakPcmBytes = primaryBudget.usedBytes();
            primary.cancel();
            pcmBytesAfterCancel = primaryBudget.usedBytes();
            postCancelRejected = !primary.accept(4L, packets.get(0));

            encoder.resetState();
            byte[] disconnectPacket = encoder.encode(frame(0));
            if (disconnectPacket == null || disconnectPacket.length == 0) {
                throw new IllegalStateException("real Opus encoder failed after resetState");
            }
            VoicePcmBudget disconnectBudget = new VoicePcmBudget(FRAME_SAMPLES * 2L);
            LoopbackSession disconnect = new LoopbackSession(disconnectDecoder, disconnectBudget);
            requireAccepted(disconnect.accept(0L, disconnectPacket), "disconnect sequence 0");
            disconnect.disconnect();
            pcmBytesAfterDisconnect = disconnectBudget.usedBytes();
            postDisconnectRejected = !disconnect.accept(1L, disconnectPacket);
        } finally {
            if (!primaryDecoder.isClosed()) {
                primaryDecoder.close();
            }
            if (!disconnectDecoder.isClosed()) {
                disconnectDecoder.close();
            }
            if (!encoder.isClosed()) {
                encoder.close();
            }
        }

        return new ProductionAcceptanceVoiceTransportState.Report(
                SAMPLE_RATE,
                FRAME_SAMPLES,
                ENCODED_FRAMES,
                encodedBytes,
                acceptedPackets,
                decodedFrames,
                decodedSamples,
                lostPackets,
                plcSamples,
                duplicateRejected,
                outOfOrderRejected,
                budgetExhaustionRejected,
                PCM_BUDGET_BYTES,
                peakPcmBytes,
                pcmBytesAfterCancel,
                pcmBytesAfterDisconnect,
                postCancelRejected,
                postDisconnectRejected,
                encoder.isClosed(),
                primaryDecoder.isClosed(),
                disconnectDecoder.isClosed()
        );
    }

    private static short[] frame(int index) {
        short[] samples = new short[FRAME_SAMPLES];
        int offset = index * FRAME_SAMPLES;
        for (int sample = 0; sample < samples.length; sample++) {
            double angle = 2.0D * Math.PI * 440.0D * (offset + sample) / SAMPLE_RATE;
            samples[sample] = (short) Math.round(Math.sin(angle) * 12_000.0D);
        }
        return samples;
    }

    private static OpusEncoder requireEncoder(OpusEncoder encoder) {
        if (encoder == null) {
            throw new IllegalStateException("Simple Voice Chat did not provide an Opus encoder");
        }
        return encoder;
    }

    private static OpusDecoder requireDecoder(OpusDecoder decoder, String label) {
        if (decoder == null) {
            throw new IllegalStateException(
                    "Simple Voice Chat did not provide the " + label + " Opus decoder"
            );
        }
        return decoder;
    }

    private static void requireAccepted(boolean accepted, String label) {
        if (!accepted) {
            throw new IllegalStateException("loopback transport rejected " + label);
        }
    }

    private static final class LoopbackSession {
        private final OpusDecoder decoder;
        private final VoicePcmBudget budget;
        private long nextSequence;
        private long lastAccepted = -1L;
        private long reservedBytes;
        private int acceptedPackets;
        private int decodedFrames;
        private int decodedSamples;
        private int lostPackets;
        private int plcSamples;
        private boolean cancelled;
        private boolean disconnected;

        private LoopbackSession(OpusDecoder decoder, VoicePcmBudget budget) {
            this.decoder = decoder;
            this.budget = budget;
        }

        private boolean accept(long sequence, byte[] packet) {
            if (cancelled || disconnected || decoder.isClosed()) {
                return false;
            }
            if (sequence < nextSequence) {
                return false;
            }
            while (nextSequence < sequence) {
                short[] concealed = decoder.decode(null);
                append(concealed, true);
                lostPackets++;
                nextSequence++;
            }
            short[] decoded = decoder.decode(packet);
            append(decoded, false);
            acceptedPackets++;
            lastAccepted = sequence;
            nextSequence = sequence + 1L;
            return true;
        }

        private void append(short[] frame, boolean concealed) {
            if (frame == null || frame.length != FRAME_SAMPLES) {
                throw new IllegalStateException(
                        "real Opus decoder returned "
                                + (frame == null ? "null" : frame.length)
                                + " samples; expected " + FRAME_SAMPLES
                );
            }
            long bytes = Math.multiplyExact((long) frame.length, 2L);
            if (!budget.tryReserve(bytes)) {
                throw new IllegalStateException("decoded PCM exceeded the bounded acceptance budget");
            }
            reservedBytes += bytes;
            decodedFrames++;
            decodedSamples += frame.length;
            if (concealed) {
                plcSamples += frame.length;
            }
        }

        private void cancel() {
            cancelled = true;
            close();
        }

        private void disconnect() {
            disconnected = true;
            close();
        }

        private void close() {
            if (!decoder.isClosed()) {
                decoder.close();
            }
            if (reservedBytes > 0L) {
                long release = reservedBytes;
                reservedBytes = 0L;
                budget.release(release);
            }
        }
    }
}
