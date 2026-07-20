package net.conczin.mca.livingworld;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.conczin.mca.MCA;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/** Server-side LivingWorld configuration. */
public final class LivingWorldConfig {
    private static final int VERSION = 1;
    private static final String OPENAI_API_KEY_ENV = "OPENAI_API_KEY";

    @SuppressWarnings("unused")
    public String README = "docs/livingworld/CONFIGURATION.md";
    public int version = VERSION;
    public boolean enabled = true;
    public String apiKey = "";
    public String provider = "openai";

    public String endpoint = "https://api.openai.com/v1/chat/completions";
    public String model = "gpt-4.1-mini";

    public boolean voiceEnabled = true;
    public String sttEndpoint = "https://api.openai.com/v1/audio/transcriptions";
    public String sttModel = "gpt-4o-mini-transcribe";
    public String sttLanguage = "";
    public String ttsEndpoint = "https://api.openai.com/v1/audio/speech";
    public String ttsModel = "tts-1";
    public String ttsVoice = "marin";
    public int voiceSilenceMillis = 800;
    public int voiceMinMillis = 250;
    public int voiceMaxSeconds = 20;
    public float voiceDistance = 32.0f;

    public int connectTimeoutSeconds = 10;
    public int readTimeoutSeconds = 60;

    public static LivingWorldConfig getInstance() {
        return Holder.INSTANCE;
    }

    public static File getConfigFile() {
        return new File("./config/livingworld.json");
    }

    public String resolvedApiKey() {
        return resolveApiKey(System.getenv(OPENAI_API_KEY_ENV), apiKey);
    }

    public boolean isConfigured() {
        return isConfiguredWithKey(resolvedApiKey());
    }

    public boolean isVoiceConfigured() {
        return voiceEnabled && isConfigured();
    }

    boolean isConfiguredWithKey(String resolvedKey) {
        return enabled
                && "openai".equals(provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT))
                && resolvedKey != null
                && !resolvedKey.isBlank();
    }

    static String resolveApiKey(String environmentKey, String configuredKey) {
        if (environmentKey != null && !environmentKey.isBlank()) return environmentKey.trim();
        return configuredKey == null ? "" : configuredKey.trim();
    }

    private static LivingWorldConfig loadOrCreate() {
        File file = getConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                LivingWorldConfig config = gson().fromJson(reader, LivingWorldConfig.class);
                if (config != null && config.version == VERSION) {
                    config.normalize();
                    config.save();
                    return config;
                }
                MCA.LOGGER.warn("LivingWorld config version is missing or unsupported; creating fresh defaults");
            } catch (JsonSyntaxException e) {
                MCA.LOGGER.error("LivingWorld config is invalid JSON; creating fresh defaults", e);
            } catch (IOException e) {
                MCA.LOGGER.error("Unable to read LivingWorld config; creating fresh defaults", e);
            }
        }
        LivingWorldConfig config = new LivingWorldConfig();
        config.save();
        return config;
    }

    private void normalize() {
        if (apiKey == null) apiKey = "";
        if (provider == null || provider.isBlank()) provider = "openai";
        if (endpoint == null || endpoint.isBlank()) endpoint = "https://api.openai.com/v1/chat/completions";
        if (model == null || model.isBlank()) model = "gpt-4.1-mini";
        if (sttEndpoint == null || sttEndpoint.isBlank()) sttEndpoint = "https://api.openai.com/v1/audio/transcriptions";
        if (sttModel == null || sttModel.isBlank()) sttModel = "gpt-4o-mini-transcribe";
        if (sttLanguage == null) sttLanguage = "";
        if (ttsEndpoint == null || ttsEndpoint.isBlank()) ttsEndpoint = "https://api.openai.com/v1/audio/speech";
        if (ttsModel == null || ttsModel.isBlank()) ttsModel = "tts-1";
        if (ttsVoice == null || ttsVoice.isBlank()) ttsVoice = "marin";
        if (voiceSilenceMillis < 200) voiceSilenceMillis = 800;
        if (voiceMinMillis < 100) voiceMinMillis = 250;
        if (voiceMaxSeconds <= 0) voiceMaxSeconds = 20;
        if (voiceDistance <= 0) voiceDistance = 32.0f;
        if (connectTimeoutSeconds <= 0) connectTimeoutSeconds = 10;
        if (readTimeoutSeconds <= 0) readTimeoutSeconds = 60;
    }

    private void save() {
        File file = getConfigFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            MCA.LOGGER.warn("Could not create LivingWorld config directory: {}", parent.getAbsolutePath());
            return;
        }
        try (FileWriter writer = new FileWriter(file)) {
            gson().toJson(this, writer);
        } catch (IOException e) {
            MCA.LOGGER.error("Unable to save LivingWorld config", e);
        }
    }

    private static Gson gson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }

    private static final class Holder {
        private static final LivingWorldConfig INSTANCE = loadOrCreate();
    }
}
