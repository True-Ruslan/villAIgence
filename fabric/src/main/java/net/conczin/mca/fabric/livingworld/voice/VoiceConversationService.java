package net.conczin.mca.fabric.livingworld.voice;

import de.maxhenkel.voicechat.api.Entity;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.conczin.mca.MCA;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.chatAI.ChatAI;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.audio.PcmAudio;
import net.conczin.mca.livingworld.voice.OpenAIAudioProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class VoiceConversationService implements AutoCloseable {
    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "livingworld-voice-ai");
        thread.setDaemon(true);
        return thread;
    });
    private final OpenAIAudioProvider audioProvider = new OpenAIAudioProvider(LivingWorldConfig.getInstance());
    private final Map<UUID, AudioPlayer> activePlayback = new ConcurrentHashMap<>();
    private volatile VoicechatServerApi serverApi;

    void setServerApi(VoicechatServerApi serverApi) {
        this.serverApi = serverApi;
    }

    void process(UUID playerId, short[] microphonePcm) {
        executor.execute(() -> transcribeAndRoute(playerId, microphonePcm));
    }

    private void transcribeAndRoute(UUID playerId, short[] microphonePcm) {
        try {
            String transcript = audioProvider.transcribe(new PcmAudio(VoiceCaptureManager.VOICECHAT_SAMPLE_RATE, microphonePcm));
            if (transcript.isBlank()) return;

            Optional<MinecraftServer> optionalServer = MCA.getServer();
            if (optionalServer.isEmpty()) return;
            MinecraftServer server = optionalServer.get();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player == null) return;
                ChatAI.getActiveConversationVillager(player).ifPresent(villager ->
                        executor.execute(() -> answerAndSpeak(server, player, villager, transcript))
                );
            });
        } catch (Exception e) {
            MCA.LOGGER.warn("LivingWorld speech-to-text failed for player {}", playerId, e);
        }
    }

    private void answerAndSpeak(MinecraftServer server, ServerPlayer player, VillagerEntityMCA villager, String transcript) {
        try {
            Optional<String> answer = ChatAI.answer(player, villager, transcript);
            if (answer.isEmpty() || answer.get().isBlank()) return;
            String text = answer.get().trim();

            server.execute(() -> {
                if (!villager.isRemoved() && player.isAlive()) {
                    villager.conversationManager.addMessage(player, Component.literal(text));
                }
            });

            PcmAudio speech = audioProvider.synthesize(text).resampleTo(VoiceCaptureManager.VOICECHAT_SAMPLE_RATE);
            server.execute(() -> playSpatial(villager, speech.samples()));
        } catch (Exception e) {
            MCA.LOGGER.warn("LivingWorld voice conversation failed for player {} and villager {}", player.getUUID(), villager.getUUID(), e);
        }
    }

    private void playSpatial(VillagerEntityMCA villager, short[] samples) {
        VoicechatServerApi api = serverApi;
        if (api == null || villager.isRemoved() || samples.length == 0) return;

        Entity voiceEntity = api.fromEntity(villager);
        if (voiceEntity == null) return;
        EntityAudioChannel channel = api.createEntityAudioChannel(UUID.randomUUID(), voiceEntity);
        if (channel == null) return;
        channel.setDistance(LivingWorldConfig.getInstance().voiceDistance);

        OpusEncoder encoder = api.createEncoder();
        if (encoder == null) return;
        try {
            AudioPlayer audioPlayer = api.createAudioPlayer(channel, encoder, samples);
            if (audioPlayer == null) {
                encoder.close();
                return;
            }
            UUID villagerId = villager.getUUID();
            AudioPlayer previous = activePlayback.put(villagerId, audioPlayer);
            if (previous != null && !previous.isStopped()) previous.stopPlaying();

            audioPlayer.setOnStopped(() -> {
                activePlayback.remove(villagerId, audioPlayer);
                encoder.close();
                channel.flush();
            });
            audioPlayer.startPlaying();
        } catch (RuntimeException e) {
            encoder.close();
            throw e;
        }
    }

    @Override
    public void close() {
        activePlayback.values().forEach(player -> {
            if (!player.isStopped()) player.stopPlaying();
        });
        activePlayback.clear();
        executor.shutdownNow();
        serverApi = null;
    }
}
