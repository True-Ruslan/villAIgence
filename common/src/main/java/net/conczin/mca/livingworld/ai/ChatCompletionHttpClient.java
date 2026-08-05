package net.conczin.mca.livingworld.ai;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Minecraft-independent HTTP transport for OpenAI-compatible chat completions.
 *
 * <p>The caller owns gameplay mutation and diagnostics. This class owns only endpoint-safe HTTP,
 * bounded response reads, completion parsing and the bounded retry count.</p>
 */
public final class ChatCompletionHttpClient {
    public static final String EMPTY_RESPONSE_ERROR = "empty_response";
    public static final String REQUEST_FAILED_ERROR = "AI provider request failed; check server log";
    public static final String RESPONSE_TOO_LARGE_ERROR = "AI provider response exceeded safe size limit";

    private ChatCompletionHttpClient() {
    }

    public static Result post(
            ProviderEndpoint endpoint,
            String requestBody,
            String token,
            int connectTimeoutMillis,
            int readTimeoutMillis,
            AttemptObserver observer
    ) {
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(requestBody, "requestBody");
        Objects.requireNonNull(token, "token");
        AttemptObserver safeObserver = observer == null ? AttemptObserver.NOOP : observer;

        for (int attempt = 1; attempt <= ChatCompletionRetryPolicy.MAX_ATTEMPTS; attempt++) {
            AttemptResult result = postOnce(
                    endpoint,
                    requestBody,
                    token,
                    connectTimeoutMillis,
                    readTimeoutMillis
            );
            ChatCompletionResponseParser.ParsedCompletion completion = result.completion();

            if (result.error() != null) {
                safeObserver.onProviderFailure(attempt, completion);
                return new Result(completion, result.error(), attempt, result.failure());
            }
            if (completion != null && completion.content() != null) {
                return new Result(completion, null, attempt, null);
            }
            if (ChatCompletionRetryPolicy.shouldRetry(completion, attempt)) {
                safeObserver.onEmptyCompletion(attempt, completion, true);
                continue;
            }

            safeObserver.onEmptyCompletion(attempt, completion, false);
            return new Result(completion, EMPTY_RESPONSE_ERROR, attempt, null);
        }

        return new Result(null, EMPTY_RESPONSE_ERROR, ChatCompletionRetryPolicy.MAX_ATTEMPTS, null);
    }

    private static AttemptResult postOnce(
            ProviderEndpoint endpoint,
            String requestBody,
            String token,
            int connectTimeoutMillis,
            int readTimeoutMillis
    ) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(endpoint, token, connectTimeoutMillis, readTimeoutMillis);
            try (DataOutputStream output = new DataOutputStream(connection.getOutputStream())) {
                output.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            boolean success = status >= 200 && status < 300;
            InputStream response = success ? connection.getInputStream() : connection.getErrorStream();
            if (response == null) {
                return new AttemptResult(null, "AI provider returned HTTP " + status, null);
            }

            int limitBytes = success
                    ? ProviderResponseLimits.CHAT_JSON_BYTES
                    : ProviderResponseLimits.ERROR_BODY_BYTES;
            String body;
            try (response) {
                body = BoundedResponseReader.readUtf8(
                        response,
                        connection.getContentLengthLong(),
                        limitBytes
                );
            }

            ChatCompletionResponseParser.ParsedCompletion completion =
                    ChatCompletionResponseParser.parse(body);
            if (!success) {
                String error = completion.error() != null
                        ? completion.error()
                        : "AI provider returned HTTP " + status;
                return new AttemptResult(completion, error, null);
            }
            if (completion.error() != null) {
                return new AttemptResult(completion, completion.error(), null);
            }
            return new AttemptResult(completion, null, null);
        } catch (BoundedResponseReader.ResponseTooLargeException exception) {
            return new AttemptResult(null, RESPONSE_TOO_LARGE_ERROR, exception);
        } catch (Exception exception) {
            return new AttemptResult(null, REQUEST_FAILED_ERROR, exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection openConnection(
            ProviderEndpoint endpoint,
            String token,
            int connectTimeoutMillis,
            int readTimeoutMillis
    ) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) endpoint.uri().toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.toString());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setDoOutput(true);
        return connection;
    }

    public interface AttemptObserver {
        AttemptObserver NOOP = new AttemptObserver() {
        };

        default void onProviderFailure(
                int attempt,
                ChatCompletionResponseParser.ParsedCompletion completion
        ) {
        }

        default void onEmptyCompletion(
                int attempt,
                ChatCompletionResponseParser.ParsedCompletion completion,
                boolean retrying
        ) {
        }
    }

    public record Result(
            ChatCompletionResponseParser.ParsedCompletion completion,
            String error,
            int attempts,
            Exception failure
    ) {
    }

    private record AttemptResult(
            ChatCompletionResponseParser.ParsedCompletion completion,
            String error,
            Exception failure
    ) {
    }
}
