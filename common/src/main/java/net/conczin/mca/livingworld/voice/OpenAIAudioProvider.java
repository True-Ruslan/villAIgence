package net.conczin.mca.livingworld.voice;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.conczin.mca.livingworld.LivingWorldConfig;
import net.conczin.mca.livingworld.audio.PcmAudio;
import net.conczin.mca.livingworld.audio.WavCodec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/** OpenAI-compatible Audio API implementation used by LivingWorld voice input/output. */
public final class OpenAIAudioProvider implements SpeechToTextProvider, TextToSpeechProvider {
    private final LivingWorldConfig config;
    private final Gson gson = new Gson();

    public OpenAIAudioProvider(LivingWorldConfig config) {
        this.config = config;
    }

    @Override
    public String transcribe(PcmAudio audio) throws IOException {
        byte[] wav = WavCodec.encodePcm16Mono(audio.samples(), audio.sampleRate());
        SttRequestFormat format = SttRequestFormat.parse(config.sttRequestFormat).resolve(config.sttEndpoint);
        byte[] requestBody;
        String contentType;

        if (format == SttRequestFormat.JSON_BASE64) {
            requestBody = createJsonTranscriptionBody(wav, config.sttModel, config.sttLanguage)
                    .getBytes(StandardCharsets.UTF_8);
            contentType = "application/json";
        } else {
            String boundary = "----LivingWorld" + UUID.randomUUID().toString().replace("-", "");
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            writeFormField(body, boundary, "model", config.sttModel);
            if (config.sttLanguage != null && !config.sttLanguage.isBlank()) {
                writeFormField(body, boundary, "language", config.sttLanguage.trim());
            }
            writeFileField(body, boundary, "file", "speech.wav", "audio/wav", wav);
            body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            requestBody = body.toByteArray();
            contentType = "multipart/form-data; boundary=" + boundary;
        }

        byte[] response = execute(
                open(config.sttEndpoint, contentType, config.resolvedSttApiKey()),
                requestBody,
                "speech-to-text"
        );
        JsonObject json = JsonParser.parseString(new String(response, StandardCharsets.UTF_8)).getAsJsonObject();
        if (!json.has("text") || json.get("text").isJsonNull()) throw new IOException("STT response did not contain text");
        return json.get("text").getAsString().trim();
    }

    static String createJsonTranscriptionBody(byte[] wav, String model, String language) {
        JsonObject inputAudio = new JsonObject();
        inputAudio.addProperty("data", Base64.getEncoder().encodeToString(wav));
        inputAudio.addProperty("format", "wav");

        JsonObject request = new JsonObject();
        request.addProperty("model", model);
        request.add("input_audio", inputAudio);
        if (language != null && !language.isBlank()) request.addProperty("language", language.trim());
        return new Gson().toJson(request);
    }

    @Override
    public PcmAudio synthesize(String text) throws IOException {
        return synthesize(new TtsRequest(text, config.ttsVoice, TtsVoiceStyle.NEUTRAL));
    }

    @Override
    public PcmAudio synthesize(TtsRequest request) throws IOException {
        String voice = request.voiceId().isBlank() ? config.ttsVoice : request.voiceId();
        TtsRequest resolved = new TtsRequest(request.text(), voice, request.style());
        byte[] response = execute(
                open(config.ttsEndpoint, "application/json", config.resolvedTtsApiKey()),
                createSpeechBody(resolved, config.ttsModel).getBytes(StandardCharsets.UTF_8),
                "text-to-speech"
        );
        try {
            return WavCodec.decodePcm16Mono(response);
        } catch (IllegalArgumentException e) {
            throw new IOException("TTS provider returned an unsupported WAV payload", e);
        }
    }

    static String createSpeechBody(TtsRequest request, String model) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("voice", request.voiceId());
        body.addProperty("input", request.text());
        body.addProperty("response_format", "wav");
        body.addProperty("speed", request.style().speed());
        if (supportsInstructions(model) && !request.style().instructions().isBlank()) {
            body.addProperty("instructions", request.style().instructions());
        }
        return new Gson().toJson(body);
    }

    static boolean supportsInstructions(String model) {
        if (model == null) return false;
        String normalized = model.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("gpt-4o-mini-tts") || normalized.contains("gpt-4o-tts");
    }

    private HttpURLConnection open(String endpoint, String contentType, String apiKey) throws IOException {
        if (endpoint == null || endpoint.isBlank()) throw new IOException("AI audio endpoint is not configured");
        if (apiKey == null || apiKey.isBlank()) throw new IOException("AI audio API key is not configured");
        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Accept", "application/json, audio/wav");
        connection.setRequestProperty("Content-Type", contentType);
        if (SttRequestFormat.isOpenRouterEndpoint(endpoint)) {
            connection.setRequestProperty("X-OpenRouter-Title", "LivingWorld");
        }
        connection.setConnectTimeout(secondsToMillis(config.connectTimeoutSeconds));
        connection.setReadTimeout(secondsToMillis(config.readTimeoutSeconds));
        connection.setDoOutput(true);
        return connection;
    }

    private byte[] execute(HttpURLConnection connection, byte[] requestBody, String operation) throws IOException {
        try (OutputStream output = connection.getOutputStream()) {
            output.write(requestBody);
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        byte[] response = stream == null ? new byte[0] : readAll(stream);
        if (status < 200 || status >= 300) {
            String detail = extractError(response);
            if (status == 402 && SttRequestFormat.isOpenRouterEndpoint(connection.getURL().toString())) {
                throw new IOException("OpenRouter " + operation + " failed (HTTP 402 Payment Required): add credits to the OpenRouter account. Provider response: " + detail);
            }
            throw new IOException("AI audio " + operation + " failed (HTTP " + status + "): " + detail);
        }
        return response;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private static String extractError(byte[] response) {
        if (response.length == 0) return "empty response";
        String text = new String(response, StandardCharsets.UTF_8);
        try {
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            if (root.has("error")) {
                if (root.get("error").isJsonObject()) {
                    JsonObject error = root.getAsJsonObject("error");
                    if (error.has("message") && !error.get("message").isJsonNull()) {
                        return error.get("message").getAsString();
                    }
                    if (error.has("code") && !error.get("code").isJsonNull()) {
                        return error.get("code").getAsString();
                    }
                } else if (!root.get("error").isJsonNull()) {
                    return root.get("error").getAsString();
                }
            }
            if (root.has("message") && !root.get("message").isJsonNull()) {
                return root.get("message").getAsString();
            }
        } catch (RuntimeException ignored) {
        }
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    private static int secondsToMillis(int seconds) {
        long value = Math.max(1L, seconds) * 1_000L;
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private static void writeFormField(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFileField(ByteArrayOutputStream output, String boundary, String name, String filename, String contentType, byte[] data) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(data);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}
