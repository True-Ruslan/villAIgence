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
import java.util.UUID;

/** OpenAI Audio API implementation used by the voice MVP. */
public final class OpenAIAudioProvider implements SpeechToTextProvider, TextToSpeechProvider {
    private final LivingWorldConfig config;
    private final Gson gson = new Gson();

    public OpenAIAudioProvider(LivingWorldConfig config) {
        this.config = config;
    }

    @Override
    public String transcribe(PcmAudio audio) throws IOException {
        byte[] wav = WavCodec.encodePcm16Mono(audio.samples(), audio.sampleRate());
        String boundary = "----LivingWorld" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeFormField(body, boundary, "model", config.sttModel);
        if (config.sttLanguage != null && !config.sttLanguage.isBlank()) {
            writeFormField(body, boundary, "language", config.sttLanguage.trim());
        }
        writeFileField(body, boundary, "file", "speech.wav", "audio/wav", wav);
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        byte[] response = execute(open(config.sttEndpoint, "multipart/form-data; boundary=" + boundary), body.toByteArray());
        JsonObject json = JsonParser.parseString(new String(response, StandardCharsets.UTF_8)).getAsJsonObject();
        if (!json.has("text") || json.get("text").isJsonNull()) throw new IOException("STT response did not contain text");
        return json.get("text").getAsString().trim();
    }

    @Override
    public PcmAudio synthesize(String text) throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty("model", config.ttsModel);
        request.addProperty("voice", config.ttsVoice);
        request.addProperty("input", text);
        request.addProperty("response_format", "wav");
        byte[] response = execute(open(config.ttsEndpoint, "application/json"), gson.toJson(request).getBytes(StandardCharsets.UTF_8));
        try {
            return WavCodec.decodePcm16Mono(response);
        } catch (IllegalArgumentException e) {
            throw new IOException("TTS provider returned an unsupported WAV payload", e);
        }
    }

    private HttpURLConnection open(String endpoint, String contentType) throws IOException {
        if (endpoint == null || endpoint.isBlank()) throw new IOException("AI audio endpoint is not configured");
        String apiKey = config.resolvedApiKey();
        if (apiKey.isBlank()) throw new IOException("OpenAI API key is not configured");
        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Accept", "application/json, audio/wav");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setConnectTimeout(secondsToMillis(config.connectTimeoutSeconds));
        connection.setReadTimeout(secondsToMillis(config.readTimeoutSeconds));
        connection.setDoOutput(true);
        return connection;
    }

    private byte[] execute(HttpURLConnection connection, byte[] requestBody) throws IOException {
        try (OutputStream output = connection.getOutputStream()) {
            output.write(requestBody);
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        byte[] response = stream == null ? new byte[0] : readAll(stream);
        if (status < 200 || status >= 300) {
            throw new IOException("OpenAI audio request failed (HTTP " + status + "): " + extractError(response));
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
            if (root.has("error") && root.get("error").isJsonObject()) {
                JsonObject error = root.getAsJsonObject("error");
                if (error.has("message")) return error.get("message").getAsString();
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
