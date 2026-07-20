package net.conczin.mca.fabric.livingworld.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.conczin.mca.MCA;
import net.conczin.mca.entity.ai.chatAI.ChatAI;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class VoiceCaptureManager implements AutoCloseable {
    static final int VOICECHAT_SAMPLE_RATE = 48_000;

    private final VoicechatApi voicechatApi;
    private final VoiceConversationService conversationService;
    private final Map<UUID, CaptureSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "livingworld-voice-segmentation");
        thread.setDaemon(true);
        return thread;
    });

    VoiceCaptureManager(VoicechatApi voicechatApi) {
        this.voicechatApi = voicechatApi;
        this.conversationService = new VoiceConversationService();
        scheduler.scheduleAtFixedRate(this::flushIdleSessions, 200, 200, TimeUnit.MILLISECONDS);
    }

    void setServerApi(VoicechatServerApi serverApi) {
        conversationService.setServerApi(serverApi);
    }

    void onMicrophonePacket(MicrophonePacketEvent event) {
        LivingWorldConfig config = LivingWorldConfig.getInstance();
        if (!config.isVoiceConfigured()) return;

        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || sender.getPlayer() == null) return;
        Object rawPlayer = sender.getPlayer().getPlayer();
        if (!(rawPlayer instanceof ServerPlayer player)) return;

        UUID playerId = player.getUUID();
        if (!ChatAI.hasOpenConversation(playerId) || conversationService.isBusy(playerId)) return;

        byte[] opus = event.getPacket().getOpusEncodedData();
        if (opus == null || opus.length == 0) return;

        try {
            CaptureSession session = sessions.computeIfAbsent(playerId, ignored -> new CaptureSession(voicechatApi.createDecoder()));
            boolean full = session.append(opus, Math.multiplyExact(VOICECHAT_SAMPLE_RATE, config.voiceMaxSeconds));
            if (full && sessions.remove(playerId, session)) finish(playerId, session);
        } catch (RuntimeException e) {
            MCA.LOGGER.warn("LivingWorld failed to decode microphone audio for {}", playerId, e);
            CaptureSession removed = sessions.remove(playerId);
            if (removed != null) removed.close();
        }
    }

    private void flushIdleSessions() {
        long now = System.currentTimeMillis();
        int silenceMillis = LivingWorldConfig.getInstance().voiceSilenceMillis;
        for (Map.Entry<UUID, CaptureSession> entry : sessions.entrySet()) {
            CaptureSession session = entry.getValue();
            if (session.isIdle(now, silenceMillis) && sessions.remove(entry.getKey(), session)) finish(entry.getKey(), session);
        }
    }

    private void finish(UUID playerId, CaptureSession session) {
        short[] samples = session.finish();
        int minimumSamples = VOICECHAT_SAMPLE_RATE * LivingWorldConfig.getInstance().voiceMinMillis / 1_000;
        if (samples.length >= minimumSamples) conversationService.process(playerId, samples);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        sessions.values().forEach(CaptureSession::close);
        sessions.clear();
        conversationService.close();
    }

    private static final class CaptureSession implements AutoCloseable {
        private final OpusDecoder decoder;
        private final ByteArrayOutputStream pcmBytes = new ByteArrayOutputStream();
        private volatile long lastPacketMillis = System.currentTimeMillis();
        private int samples;
        private boolean closed;

        private CaptureSession(OpusDecoder decoder) {
            if (decoder == null) throw new IllegalStateException("Simple Voice Chat did not provide an Opus decoder");
            this.decoder = decoder;
        }

        synchronized boolean append(byte[] opus, int maxSamples) {
            if (closed) return true;
            short[] decoded = decoder.decode(opus);
            int remaining = Math.max(0, maxSamples - samples);
            int accepted = Math.min(decoded.length, remaining);
            ByteBuffer buffer = ByteBuffer.allocate(accepted * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < accepted; i++) buffer.putShort(decoded[i]);
            pcmBytes.writeBytes(buffer.array());
            samples += accepted;
            lastPacketMillis = System.currentTimeMillis();
            return samples >= maxSamples;
        }

        boolean isIdle(long nowMillis, int silenceMillis) {
            return nowMillis - lastPacketMillis >= silenceMillis;
        }

        synchronized short[] finish() {
            if (!closed) close();
            byte[] bytes = pcmBytes.toByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            short[] output = new short[bytes.length / 2];
            for (int i = 0; i < output.length; i++) output[i] = buffer.getShort();
            return output;
        }

        @Override
        public synchronized void close() {
            if (closed) return;
            closed = true;
            decoder.close();
        }
    }
}
