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
import net.conczin.mca.livingworld.context.LivingWorldContextCapture;
import net.conczin.mca.livingworld.context.LivingWorldContextSnapshot;
import net.conczin.mca.livingworld.diagnostics.AiOperation;
import net.conczin.mca.livingworld.diagnostics.VoiceDiagnosticsRecorder;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipState;
import net.conczin.mca.livingworld.relationship.LivingWorldRelationshipStore;
import net.conczin.mca.livingworld.voice.NpcVoiceAgeGroup;
import net.conczin.mca.livingworld.voice.NpcVoiceCatalog;
import net.conczin.mca.livingworld.voice.NpcVoiceGender;
import net.conczin.mca.livingworld.voice.NpcVoiceMood;
import net.conczin.mca.livingworld.voice.NpcVoiceMoodResolver;
import net.conczin.mca.livingworld.voice.NpcVoiceProfile;
import net.conczin.mca.livingworld.voice.NpcVoiceSnapshot;
import net.conczin.mca.livingworld.voice.OpenAIAudioProvider;
import net.conczin.mca.livingworld.voice.PersistentNpcVoiceStore;
import net.conczin.mca.livingworld.voice.SttRequestFormat;
import net.conczin.mca.livingworld.voice.TtsRequest;
import net.conczin.mca.livingworld.voice.TtsResponseFormat;
import net.conczin.mca.livingworld.voice.TtsVoiceStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class VoiceConversationService implements AutoCloseable {
    private static final double MIN_LOOK_DOT = 0.70D;

    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "livingworld-voice-ai");
        thread.setDaemon(true);
        return thread;
    });
    private final OpenAIAudioProvider audioProvider = new OpenAIAudioProvider(LivingWorldConfig.getInstance());
    private final Map<UUID, AudioPlayer> activePlayback = new ConcurrentHashMap<>();
    private final Set<UUID> busyPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> busyVillagers = ConcurrentHashMap.newKeySet();
    private volatile VoicechatServerApi serverApi;

    void setServerApi(VoicechatServerApi serverApi) {
        this.serverApi = serverApi;
    }

    boolean isBusy(UUID playerId) {
        return busyPlayers.contains(playerId);
    }

    void process(UUID playerId, short[] microphonePcm) {
        if (!busyPlayers.add(playerId)) return;

        Optional<MinecraftServer> optionalServer = MCA.getServer();
        if (optionalServer.isEmpty()) {
            busyPlayers.remove(playerId);
            return;
        }
        MinecraftServer server = optionalServer.get();
        server.execute(() -> validateTargetAndTranscribe(server, playerId, microphonePcm));
    }

    private void validateTargetAndTranscribe(MinecraftServer server, UUID playerId, short[] microphonePcm) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            busyPlayers.remove(playerId);
            return;
        }
        Optional<VillagerEntityMCA> target = ChatAI.getActiveConversationVillager(player);
        if (target.isEmpty() || !isAddressingTarget(player, target.get())) {
            busyPlayers.remove(playerId);
            return;
        }

        UUID targetId = target.get().getUUID();
        if (!busyVillagers.add(targetId)) {
            busyPlayers.remove(playerId);
            return;
        }
        executor.execute(() -> transcribeAndRoute(server, playerId, targetId, microphonePcm));
    }

    private void transcribeAndRoute(MinecraftServer server, UUID playerId, UUID targetId, short[] microphonePcm) {
        LivingWorldConfig config = LivingWorldConfig.getInstance();
        String sttFormat = SttRequestFormat.parse(config.sttRequestFormat).resolve(config.sttEndpoint).configValue();
        String transcript;
        long startedNanos = System.nanoTime();
        try {
            transcript = audioProvider.transcribe(new PcmAudio(VoiceCaptureManager.VOICECHAT_SAMPLE_RATE, microphonePcm));
            VoiceDiagnosticsRecorder.recordSuccess(
                    AiOperation.STT,
                    config.sttEndpoint,
                    config.sttModel,
                    sttFormat,
                    VoiceDiagnosticsRecorder.elapsedMillis(startedNanos)
            );
        } catch (Exception e) {
            VoiceDiagnosticsRecorder.recordFailure(
                    AiOperation.STT,
                    config.sttEndpoint,
                    config.sttModel,
                    sttFormat,
                    VoiceDiagnosticsRecorder.elapsedMillis(startedNanos),
                    e
            );
            release(playerId, targetId);
            MCA.LOGGER.warn("LivingWorld speech-to-text failed for player {}", playerId, e);
            return;
        }

        if (transcript.isBlank()) {
            release(playerId, targetId);
            return;
        }

        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                release(playerId, targetId);
                return;
            }
            Optional<VillagerEntityMCA> target = ChatAI.getActiveConversationVillager(player)
                    .filter(villager -> villager.getUUID().equals(targetId));
            if (target.isEmpty()) {
                release(playerId, targetId);
                return;
            }

            LivingWorldContextSnapshot snapshot;
            NpcVoiceSnapshot voiceSnapshot;
            try {
                snapshot = LivingWorldContextCapture.capture(player, target.get());
                voiceSnapshot = captureVoiceSnapshot(target.get());
            } catch (RuntimeException e) {
                MCA.LOGGER.warn("LivingWorld context snapshot failed for player {} and villager {}", playerId, targetId, e);
                release(playerId, targetId);
                return;
            }
            executor.execute(() -> answerAndRespond(server, player, target.get(), snapshot, voiceSnapshot, transcript));
        });
    }

    private static boolean isAddressingTarget(ServerPlayer player, VillagerEntityMCA villager) {
        if (!player.hasLineOfSight(villager)) return false;
        Vec3 toVillager = villager.getEyePosition().subtract(player.getEyePosition());
        if (toVillager.lengthSqr() < 1.0E-6D) return true;
        return player.getLookAngle().normalize().dot(toVillager.normalize()) >= MIN_LOOK_DOT;
    }

    private static NpcVoiceSnapshot captureVoiceSnapshot(VillagerEntityMCA villager) {
        double maxHealth = villager.getMaxHealth();
        double healthRatio = maxHealth > 0.0D ? villager.getHealth() / maxHealth : 1.0D;
        return new NpcVoiceSnapshot(
                villager.getUUID(),
                NpcVoiceGender.fromName(villager.getGenetics().getGender().getDataName()),
                NpcVoiceAgeGroup.fromName(villager.getAgeState().name()),
                villager.getVillagerBrain().isPanicking(),
                healthRatio
        );
    }

    private void answerAndRespond(
            MinecraftServer server,
            ServerPlayer player,
            VillagerEntityMCA villager,
            LivingWorldContextSnapshot snapshot,
            NpcVoiceSnapshot voiceSnapshot,
            String transcript
    ) {
        UUID playerId = snapshot.playerId();
        UUID villagerId = snapshot.villagerId();
        try {
            Optional<String> answer = ChatAI.answer(server, player, villager, transcript, snapshot);
            if (answer.isEmpty() || answer.get().isBlank()) return;
            String text = answer.get().trim();

            server.execute(() -> {
                if (!villager.isRemoved() && player.isAlive()) {
                    villager.conversationManager.addMessage(player, Component.literal(text));
                }
            });

            LivingWorldConfig config = LivingWorldConfig.getInstance();
            if (!config.isVoiceOutputConfigured()) return;

            NpcVoiceCatalog catalog = new NpcVoiceCatalog(config.voicePools());
            NpcVoiceProfile profile = PersistentNpcVoiceStore.forWorld(snapshot.worldRoot()).resolve(
                    voiceSnapshot.npcId(),
                    voiceSnapshot.gender(),
                    voiceSnapshot.ageGroup(),
                    catalog
            );
            LivingWorldRelationshipState relationship = LivingWorldRelationshipStore.forWorld(snapshot.worldRoot())
                    .get(villagerId, playerId);
            NpcVoiceMood mood = NpcVoiceMoodResolver.resolve(
                    voiceSnapshot.panicking(),
                    voiceSnapshot.healthRatio(),
                    relationship.trust(),
                    relationship.fear(),
                    relationship.affinity()
            );
            TtsVoiceStyle style = NpcVoiceMoodResolver.style(mood, voiceSnapshot.ageGroup());
            String ttsFormat = TtsResponseFormat.parse(config.ttsResponseFormat).resolve(config.ttsEndpoint).configValue();
            PcmAudio speech;
            long startedNanos = System.nanoTime();
            try {
                speech = audioProvider.synthesize(new TtsRequest(text, profile.voiceId(), style))
                        .resampleTo(VoiceCaptureManager.VOICECHAT_SAMPLE_RATE);
                VoiceDiagnosticsRecorder.recordSuccess(
                        AiOperation.TTS,
                        config.ttsEndpoint,
                        config.ttsModel,
                        ttsFormat,
                        VoiceDiagnosticsRecorder.elapsedMillis(startedNanos)
                );
            } catch (Exception e) {
                VoiceDiagnosticsRecorder.recordFailure(
                        AiOperation.TTS,
                        config.ttsEndpoint,
                        config.ttsModel,
                        ttsFormat,
                        VoiceDiagnosticsRecorder.elapsedMillis(startedNanos),
                        e
                );
                throw e;
            }
            server.execute(() -> playSpatial(villager, speech.samples()));
        } catch (Exception e) {
            MCA.LOGGER.warn("LivingWorld voice conversation failed for player {} and villager {}", playerId, villagerId, e);
        } finally {
            release(playerId, villagerId);
        }
    }

    private void release(UUID playerId, UUID villagerId) {
        busyPlayers.remove(playerId);
        busyVillagers.remove(villagerId);
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
        busyPlayers.clear();
        busyVillagers.clear();
        executor.shutdownNow();
        serverApi = null;
    }
}
