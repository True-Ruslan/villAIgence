package net.conczin.mca.fabric.livingworld.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

public final class LivingWorldVoicechatPlugin implements VoicechatPlugin {
    private volatile VoiceCaptureManager captureManager;

    @Override
    public String getPluginId() {
        return "mca_livingworld";
    }

    @Override
    public void initialize(VoicechatApi api) {
        captureManager = new VoiceCaptureManager(api);
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> manager().setServerApi(event.getVoicechat()));
        registration.registerEvent(MicrophonePacketEvent.class, event -> manager().onMicrophonePacket(event));
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> manager().close());
    }

    private VoiceCaptureManager manager() {
        VoiceCaptureManager manager = captureManager;
        if (manager == null) {
            throw new IllegalStateException("LivingWorld voice plugin was not initialized");
        }
        return manager;
    }
}
