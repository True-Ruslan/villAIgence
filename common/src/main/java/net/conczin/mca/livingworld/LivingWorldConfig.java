package net.conczin.mca.livingworld;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.conczin.mca.Config;
import net.conczin.mca.MCA;
import net.conczin.mca.livingworld.actions.LivingWorldActionPolicy;
import net.conczin.mca.livingworld.voice.NpcVoiceCatalog;
import net.conczin.mca.livingworld.voice.SttRequestFormat;
import net.conczin.mca.livingworld.voice.TtsResponseFormat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Server-side LivingWorld configuration. */
public final class LivingWorldConfig {
    private static final int VERSION = 2;
    private static final String OPENAI_API_KEY_ENV = "OPENAI_API_KEY";
    private static final String OPENROUTER_API_KEY_ENV = "OPENROUTER_API_KEY";

    @SuppressWarnings("unused")
    public String README = "docs/livingworld/CONFIGURATION.md";
    public int version = VERSION;
    public boolean enabled = true;
    public String apiKey = "";
    public String provider = "openai";

    public String endpoint = "https://api.openai.com/v1/chat/completions";
    public String model = "gpt-4.1-mini";

    public boolean safeActionsEnabled = true;

    public boolean persistentMemoryEnabled = true;
    public int persistentMemoryMaxMessages = 16;
    public int persistentMemoryMaxCharsPerMessage = 1200;

    public boolean eventMemoryEnabled = true;
    public int eventMemoryMaxEvents = 512;
    public long eventMemoryMaxAgeTicks = 72_000L;
    public double eventContextRadius = 32.0D;
    public int eventContextMaxEvents = 8;

    public boolean relationshipStateEnabled = true;
    public int relationshipMaxDeltaPerTurn = 2;

    /** Capture microphone audio and route it through STT into the NPC AI. */
    public boolean voiceInputEnabled = true;
    /** Synthesize NPC answers and play them spatially. Text answers are always shown. */
    public boolean voiceOutputEnabled = false;
    /** auto, multipart, or json_base64. */
    public String sttRequestFormat = "auto";
    /** Optional dedicated STT key. Prefer OPENROUTER_API_KEY for OpenRouter. */
    public String sttApiKey = "";
    public String sttEndpoint = "https://api.openai.com/v1/audio/transcriptions";
    public String sttModel = "gpt-4o-mini-transcribe";
    public String sttLanguage = "";
    /** Optional dedicated TTS key. Endpoint-specific environment keys take priority. */
    public String ttsApiKey = "";
    public String ttsEndpoint = "https://api.openai.com/v1/audio/speech";
    public String ttsModel = "tts-1";
    /** Legacy/final fallback voice. Persistent NPC profiles use the pools below first. */
    public String ttsVoice = "marin";
    /** auto, wav, or pcm. */
    public String ttsResponseFormat = "auto";
    /** Fallback sample rate for raw PCM responses when the Content-Type omits a rate parameter. */
    public int ttsPcmSampleRate = 24_000;

    /** LivingWorld defaults only; providers do not formally classify built-in voices by gender/age. */
    public List<String> maleChildVoices = List.of("ash", "echo");
    public List<String> femaleChildVoices = List.of("shimmer", "coral");
    public List<String> neutralChildVoices = List.of("alloy", "verse");
    public List<String> maleTeenVoices = List.of("ash", "echo", "cedar");
    public List<String> femaleTeenVoices = List.of("coral", "nova", "shimmer");
    public List<String> neutralTeenVoices = List.of("alloy", "verse");
    public List<String> maleAdultVoices = List.of("cedar", "onyx", "echo", "ash");
    public List<String> femaleAdultVoices = List.of("marin", "coral", "nova", "shimmer", "sage");
    public List<String> neutralAdultVoices = List.of("alloy", "verse", "fable", "ballad");
    public List<String> globalVoiceFallbacks = List.of("marin", "cedar", "alloy");

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
        return resolveProviderApiKey(
                provider,
                System.getenv(OPENROUTER_API_KEY_ENV),
                System.getenv(OPENAI_API_KEY_ENV),
                apiKey
        );
    }

    public String resolvedSttApiKey() {
        return resolveSttApiKey(
                sttEndpoint,
                System.getenv(OPENROUTER_API_KEY_ENV),
                sttApiKey,
                resolvedApiKey()
        );
    }

    public String resolvedTtsApiKey() {
        return resolveTtsApiKey(
                ttsEndpoint,
                System.getenv(OPENROUTER_API_KEY_ENV),
                System.getenv(OPENAI_API_KEY_ENV),
                ttsApiKey,
                provider,
                resolvedApiKey()
        );
    }

    public boolean isConfigured() {
        return isConfiguredWithKey(resolvedApiKey());
    }

    public boolean isVoiceInputConfigured() {
        return voiceInputEnabled && isConfigured() && !resolvedSttApiKey().isBlank();
    }

    public boolean isVoiceOutputConfigured() {
        return voiceOutputEnabled && isConfigured() && !resolvedTtsApiKey().isBlank();
    }

    /** Compatibility helper for callers that only need to know whether either voice direction is enabled. */
    public boolean isVoiceConfigured() {
        return isVoiceInputConfigured() || isVoiceOutputConfigured();
    }

    public NpcVoiceCatalog.VoicePools voicePools() {
        return new NpcVoiceCatalog.VoicePools(
                normalizeVoiceList(maleChildVoices),
                normalizeVoiceList(femaleChildVoices),
                normalizeVoiceList(neutralChildVoices),
                normalizeVoiceList(maleTeenVoices),
                normalizeVoiceList(femaleTeenVoices),
                normalizeVoiceList(neutralTeenVoices),
                normalizeVoiceList(maleAdultVoices),
                normalizeVoiceList(femaleAdultVoices),
                normalizeVoiceList(neutralAdultVoices),
                normalizeVoiceList(globalVoiceFallbacks),
                ttsVoice == null ? "" : ttsVoice.trim()
        );
    }

    boolean isConfiguredWithKey(String resolvedKey) {
        String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        return enabled
                && ("openai".equals(normalizedProvider) || "openrouter".equals(normalizedProvider))
                && resolvedKey != null
                && !resolvedKey.isBlank();
    }

    static String resolveProviderApiKey(
            String provider,
            String openRouterEnvironmentKey,
            String openAiEnvironmentKey,
            String configuredKey
    ) {
        String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if ("openrouter".equals(normalizedProvider)) {
            if (openRouterEnvironmentKey != null && !openRouterEnvironmentKey.isBlank()) {
                return openRouterEnvironmentKey.trim();
            }
        } else if (openAiEnvironmentKey != null && !openAiEnvironmentKey.isBlank()) {
            return openAiEnvironmentKey.trim();
        }
        return configuredKey == null ? "" : configuredKey.trim();
    }

    static String resolveSttApiKey(
            String endpoint,
            String openRouterEnvironmentKey,
            String configuredSttKey,
            String mainProviderKey
    ) {
        if (SttRequestFormat.isOpenRouterEndpoint(endpoint)
                && openRouterEnvironmentKey != null
                && !openRouterEnvironmentKey.isBlank()) {
            return openRouterEnvironmentKey.trim();
        }
        if (configuredSttKey != null && !configuredSttKey.isBlank()) return configuredSttKey.trim();
        return mainProviderKey == null ? "" : mainProviderKey.trim();
    }

    static String resolveTtsApiKey(
            String endpoint,
            String openRouterEnvironmentKey,
            String openAiEnvironmentKey,
            String configuredTtsKey,
            String mainProvider,
            String mainProviderKey
    ) {
        String normalizedProvider = mainProvider == null ? "" : mainProvider.trim().toLowerCase(Locale.ROOT);

        if (TtsResponseFormat.isOpenRouterEndpoint(endpoint)) {
            if (openRouterEnvironmentKey != null && !openRouterEnvironmentKey.isBlank()) {
                return openRouterEnvironmentKey.trim();
            }
            if (configuredTtsKey != null && !configuredTtsKey.isBlank()) return configuredTtsKey.trim();
            if (!"openrouter".equals(normalizedProvider)) return "";
            return mainProviderKey == null ? "" : mainProviderKey.trim();
        }

        if (isOpenAiEndpoint(endpoint)) {
            if (openAiEnvironmentKey != null && !openAiEnvironmentKey.isBlank()) {
                return openAiEnvironmentKey.trim();
            }
            if (configuredTtsKey != null && !configuredTtsKey.isBlank()) return configuredTtsKey.trim();
            if (!"openai".equals(normalizedProvider)) return "";
            return mainProviderKey == null ? "" : mainProviderKey.trim();
        }

        if (configuredTtsKey != null && !configuredTtsKey.isBlank()) return configuredTtsKey.trim();
        return mainProviderKey == null ? "" : mainProviderKey.trim();
    }

    private static boolean isOpenAiEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return false;
        try {
            String host = URI.create(endpoint.trim()).getHost();
            if (host == null) return false;
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return normalizedHost.equals("api.openai.com") || normalizedHost.endsWith(".api.openai.com");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    static LivingWorldConfig parseJson(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            int storedVersion = root.has("version") && !root.get("version").isJsonNull()
                    ? root.get("version").getAsInt()
                    : 1;
            if (storedVersion != 1 && storedVersion != VERSION) {
                throw new JsonSyntaxException("Unsupported LivingWorld config version: " + storedVersion);
            }

            LivingWorldConfig config = gson().fromJson(root, LivingWorldConfig.class);
            if (config == null) config = new LivingWorldConfig();

            if (storedVersion == 1) {
                boolean legacyVoiceEnabled = !root.has("voiceEnabled")
                        || root.get("voiceEnabled").isJsonNull()
                        || root.get("voiceEnabled").getAsBoolean();
                config.voiceInputEnabled = legacyVoiceEnabled;
                config.voiceOutputEnabled = legacyVoiceEnabled;
            }

            config.version = VERSION;
            config.normalize();
            return config;
        } catch (RuntimeException e) {
            if (e instanceof JsonSyntaxException syntaxException) throw syntaxException;
            throw new JsonSyntaxException("Invalid LivingWorld config", e);
        }
    }

    private static LivingWorldConfig loadOrCreate() {
        File file = getConfigFile();
        if (file.exists()) {
            try {
                LivingWorldConfig config = parseJson(Files.readString(file.toPath(), StandardCharsets.UTF_8));
                applyRuntimeCompatibility(config);
                config.save();
                return config;
            } catch (JsonSyntaxException e) {
                MCA.LOGGER.error("LivingWorld config is invalid or unsupported; creating fresh defaults", e);
            } catch (IOException e) {
                MCA.LOGGER.error("Unable to read LivingWorld config; creating fresh defaults", e);
            }
        }
        LivingWorldConfig config = new LivingWorldConfig();
        config.normalize();
        applyRuntimeCompatibility(config);
        config.save();
        return config;
    }

    private static void applyRuntimeCompatibility(LivingWorldConfig livingWorld) {
        Config mca = Config.getInstance();
        mca.villagerChatAIUseTools = LivingWorldActionPolicy.shouldExposeTools(
                livingWorld.isConfigured(),
                livingWorld.safeActionsEnabled,
                mca.villagerChatAIUseTools
        );
    }

    private void normalize() {
        if (apiKey == null) apiKey = "";
        if (provider == null || provider.isBlank()) provider = "openai";
        provider = provider.trim().toLowerCase(Locale.ROOT);
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "openrouter".equals(provider)
                    ? "https://openrouter.ai/api/v1/chat/completions"
                    : "https://api.openai.com/v1/chat/completions";
        }
        if (model == null || model.isBlank()) {
            model = "openrouter".equals(provider) ? "openai/gpt-4.1-mini" : "gpt-4.1-mini";
        }
        if (persistentMemoryMaxMessages < 2) persistentMemoryMaxMessages = 16;
        if (persistentMemoryMaxCharsPerMessage < 1) persistentMemoryMaxCharsPerMessage = 1200;
        if (eventMemoryMaxEvents < 1) eventMemoryMaxEvents = 512;
        if (eventMemoryMaxAgeTicks < 1L) eventMemoryMaxAgeTicks = 72_000L;
        if (eventContextRadius < 0.0D) eventContextRadius = 32.0D;
        if (eventContextMaxEvents < 1) eventContextMaxEvents = 8;
        if (relationshipMaxDeltaPerTurn < 0) relationshipMaxDeltaPerTurn = 2;
        if (sttApiKey == null) sttApiKey = "";
        sttRequestFormat = SttRequestFormat.parse(sttRequestFormat).configValue();
        if (sttEndpoint == null || sttEndpoint.isBlank()) {
            sttEndpoint = "openrouter".equals(provider)
                    ? "https://openrouter.ai/api/v1/audio/transcriptions"
                    : "https://api.openai.com/v1/audio/transcriptions";
        }
        if (sttModel == null || sttModel.isBlank()) {
            sttModel = SttRequestFormat.isOpenRouterEndpoint(sttEndpoint)
                    ? "openai/gpt-4o-mini-transcribe"
                    : "gpt-4o-mini-transcribe";
        }
        if (sttLanguage == null) sttLanguage = "";
        if (ttsApiKey == null) ttsApiKey = "";
        if (ttsEndpoint == null || ttsEndpoint.isBlank()) ttsEndpoint = "https://api.openai.com/v1/audio/speech";
        if (ttsModel == null || ttsModel.isBlank()) ttsModel = "tts-1";
        if (ttsVoice == null || ttsVoice.isBlank()) ttsVoice = "marin";
        ttsResponseFormat = TtsResponseFormat.parse(ttsResponseFormat).configValue();
        if (ttsPcmSampleRate <= 0) ttsPcmSampleRate = 24_000;
        maleChildVoices = normalizeVoiceList(maleChildVoices);
        femaleChildVoices = normalizeVoiceList(femaleChildVoices);
        neutralChildVoices = normalizeVoiceList(neutralChildVoices);
        maleTeenVoices = normalizeVoiceList(maleTeenVoices);
        femaleTeenVoices = normalizeVoiceList(femaleTeenVoices);
        neutralTeenVoices = normalizeVoiceList(neutralTeenVoices);
        maleAdultVoices = normalizeVoiceList(maleAdultVoices);
        femaleAdultVoices = normalizeVoiceList(femaleAdultVoices);
        neutralAdultVoices = normalizeVoiceList(neutralAdultVoices);
        globalVoiceFallbacks = normalizeVoiceList(globalVoiceFallbacks);
        if (voiceSilenceMillis < 200) voiceSilenceMillis = 800;
        if (voiceMinMillis < 100) voiceMinMillis = 250;
        if (voiceMaxSeconds <= 0) voiceMaxSeconds = 20;
        if (voiceDistance <= 0) voiceDistance = 32.0f;
        if (connectTimeoutSeconds <= 0) connectTimeoutSeconds = 10;
        if (readTimeoutSeconds <= 0) readTimeoutSeconds = 60;
    }

    private static List<String> normalizeVoiceList(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) normalized.add(value.trim());
        }
        return List.copyOf(normalized);
    }

    private void save() {
        File file = getConfigFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            MCA.LOGGER.warn("Could not create LivingWorld config directory: {}", parent.getAbsolutePath());
            return;
        }
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
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
