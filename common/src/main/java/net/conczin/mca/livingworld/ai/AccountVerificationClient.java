package net.conczin.mca.livingworld.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Narrow legacy MCA account-verification client.
 *
 * <p>The public request target is derived internally from a validated Conczin Chat endpoint. Callers
 * cannot supply an arbitrary verification URL. A package-private transport entry point exists only so
 * the exact bounded/no-redirect implementation can be exercised by the local acceptance probe.</p>
 */
public final class AccountVerificationClient {
    private static final String VERIFY_PATH = "/v1/mca/verify";

    private AccountVerificationClient() {
    }

    public static Result verify(
            String configuredChatEndpoint,
            String email,
            String playerName,
            int connectTimeoutMillis,
            int readTimeoutMillis
    ) {
        try {
            ProviderEndpoint chatEndpoint = ProviderEndpointPolicy.parse(configuredChatEndpoint, false);
            URI verificationUri = buildVerificationUri(chatEndpoint, email, playerName);
            return execute(verificationUri, connectTimeoutMillis, readTimeoutMillis);
        } catch (RuntimeException ignored) {
            return Result.ERROR;
        }
    }

    static Result execute(
            URI verificationUri,
            int connectTimeoutMillis,
            int readTimeoutMillis
    ) {
        try {
            HttpURLConnection connection = (HttpURLConnection) verificationUri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
            connection.setConnectTimeout(Math.max(1_000, connectTimeoutMillis));
            connection.setReadTimeout(Math.max(1_000, readTimeoutMillis));

            int status = connection.getResponseCode();
            boolean success = status >= 200 && status < 300;
            InputStream stream = success ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) return Result.ERROR;

            String body;
            try (stream) {
                body = BoundedResponseReader.readUtf8(
                        stream,
                        connection.getContentLengthLong(),
                        success
                                ? ProviderResponseLimits.VERIFICATION_JSON_BYTES
                                : ProviderResponseLimits.ERROR_BODY_BYTES
                );
            } finally {
                connection.disconnect();
            }
            if (!success) return Result.ERROR;
            return parseResult(body);
        } catch (IOException | RuntimeException ignored) {
            return Result.ERROR;
        }
    }

    static URI buildVerificationUri(ProviderEndpoint chatEndpoint, String email, String playerName) {
        if (!chatEndpoint.trustedConczin()
                || !"https".equalsIgnoreCase(chatEndpoint.uri().getScheme())) {
            throw new IllegalArgumentException("Account verification requires the trusted Conczin HTTPS endpoint");
        }

        String query = "email=" + encode(email) + "&player=" + encode(playerName);
        String authority = chatEndpoint.host();
        if (chatEndpoint.uri().getPort() >= 0) {
            authority += ":" + chatEndpoint.uri().getPort();
        }
        return URI.create("https://" + authority + VERIFY_PATH + "?" + query);
    }

    static Result parseResult(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("answer") || json.get("answer").isJsonNull()) return Result.ERROR;
            return switch (json.get("answer").getAsString().trim().toLowerCase(Locale.ROOT)) {
                case "success" -> Result.SUCCESS;
                case "failed" -> Result.FAILED;
                default -> Result.ERROR;
            };
        } catch (RuntimeException ignored) {
            return Result.ERROR;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    public enum Result {
        SUCCESS,
        FAILED,
        ERROR
    }
}
