package com.kairo.app.core;

import com.kairo.app.data.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Case-insensitive keyword search over a conversation, returning redacted snippets. */
public final class ConversationSearch {
    private ConversationSearch() {
    }

    public static final class Hit {
        private final int index;
        private final String role;
        private final String snippet;

        public Hit(int index, String role, String snippet) {
            this.index = index;
            this.role = role == null ? "" : role;
            this.snippet = snippet == null ? "" : snippet;
        }

        public int getIndex() {
            return index;
        }

        public String getRole() {
            return role;
        }

        public String getSnippet() {
            return snippet;
        }

        public String label() {
            String who = "user".equals(role) ? "You" : "Kairo";
            return who + " · " + snippet;
        }
    }

    public static List<Hit> search(List<ChatMessage> messages, String query) {
        List<Hit> hits = new ArrayList<>();
        if (messages == null || query == null) return hits;
        String needle = query.trim().toLowerCase(Locale.US);
        if (needle.isEmpty()) return hits;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message == null) continue;
            String content = message.getContent() == null ? "" : message.getContent();
            String hay = content.toLowerCase(Locale.US);
            int at = hay.indexOf(needle);
            if (at < 0) continue;
            hits.add(new Hit(i, message.getRole(), snippetAround(content, at, needle.length())));
        }
        return hits;
    }

    public static String snippetAround(String content, int matchStart, int matchLength) {
        if (content == null) return "";
        int start = Math.max(0, matchStart - 32);
        int end = Math.min(content.length(), matchStart + Math.max(matchLength, 1) + 48);
        String snippet = content.substring(start, end).replaceAll("\\s+", " ").trim();
        if (start > 0) snippet = "…" + snippet;
        if (end < content.length()) snippet = snippet + "…";
        return ApiKeyDetector.redact(snippet);
    }
}
