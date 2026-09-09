package com.kairo.app.core;

/**
 * Rough on-device token estimate. ~4 characters per token is a widely used English heuristic,
 * not a tokenizer. Displayed as "~N tokens (estimate)".
 */
public final class TokenEstimator {
    private TokenEstimator() {
    }

    public static int estimate(String text) {
        if (text == null) return 0;
        int length = text.length();
        if (length == 0) return 0;
        return Math.max(1, length / 4);
    }

    public static int estimateConversation(Iterable<?> messages, ContentReader reader) {
        if (messages == null || reader == null) return 0;
        int total = 0;
        for (Object message : messages) {
            total += estimate(reader.content(message));
        }
        return total;
    }

    public static String label(int tokens) {
        if (tokens <= 0) return "~0 tokens (estimate)";
        return "~" + Formatters.compactCount(tokens) + " tokens (estimate)";
    }

    public interface ContentReader {
        String content(Object message);
    }
}
