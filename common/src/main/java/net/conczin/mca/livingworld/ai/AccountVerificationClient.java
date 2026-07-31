package net.conczin.mca.livingworld.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Narrow legacy MCA account-verification client.
 *
 * <p>The public request target is derived internally from a validated Conczin Chat endpoint. Callers
 * cannot supply an arbitrary verification URL. A package-private execution entry point exists for
 * automated production-transport regression tests only.</p>
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
            AccountVerificationTransport.Response response = AccountVerificationTransport.execute(
                    verificationUri,
                    connectTimeoutMillis,
                    readTimeoutMillis
            );
            if (!response.success()) return Result.ERROR;
            return parseResult(response.body());
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
