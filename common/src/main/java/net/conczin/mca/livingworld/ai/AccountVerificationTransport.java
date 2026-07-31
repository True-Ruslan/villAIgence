package net.conczin.mca.livingworld.ai;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** JDK-only bounded/no-redirect HTTP transport shared by production verification and its probe. */
final class AccountVerificationTransport {
    private AccountVerificationTransport() {
    }

    static Response execute(
            URI verificationUri,
            int connectTimeoutMillis,
            int readTimeoutMillis
    ) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) verificationUri.toURL().openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
            connection.setConnectTimeout(Math.max(1_000, connectTimeoutMillis));
            connection.setReadTimeout(Math.max(1_000, readTimeoutMillis));

            int status = connection.getResponseCode();
            boolean success = status >= 200 && status < 300;
            InputStream stream = success ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) return new Response(status, success, "");

            try (stream) {
                String body = BoundedResponseReader.readUtf8(
                        stream,
                        connection.getContentLengthLong(),
                        success
                                ? ProviderResponseLimits.VERIFICATION_JSON_BYTES
                                : ProviderResponseLimits.ERROR_BODY_BYTES
                );
                return new Response(status, success, body);
            }
        } finally {
            connection.disconnect();
        }
    }

    record Response(int status, boolean success, String body) {
    }
}
