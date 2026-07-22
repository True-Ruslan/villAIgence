package net.conczin.mca.livingworld.ai;

/** Retry policy for successful HTTP responses that contain no usable assistant content. */
public final class ChatCompletionRetryPolicy {
    /** Initial request plus one retry. */
    public static final int MAX_ATTEMPTS = 2;

    private ChatCompletionRetryPolicy() {
    }

    public static boolean shouldRetry(ChatCompletionResponseParser.ParsedCompletion completion, int attemptNumber) {
        if (completion == null) return false;
        return attemptNumber > 0
                && attemptNumber < MAX_ATTEMPTS
                && completion.retryableEmptyContent();
    }
}
