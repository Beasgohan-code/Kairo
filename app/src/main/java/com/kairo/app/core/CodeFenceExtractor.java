package com.kairo.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pulls fenced markdown code blocks out of model answers without executing them. */
public final class CodeFenceExtractor {
    private CodeFenceExtractor() {
    }

    public static final class Fence {
        private final String language;
        private final String body;

        public Fence(String language, String body) {
            this.language = language == null ? "" : language.trim();
            this.body = body == null ? "" : body;
        }

        public String getLanguage() {
            return language;
        }

        public String getBody() {
            return body;
        }
    }

    public static List<Fence> all(String text) {
        List<Fence> fences = new ArrayList<>();
        if (text == null || text.isEmpty()) return fences;
        int cursor = 0;
        while (cursor < text.length()) {
            int open = text.indexOf("```", cursor);
            if (open < 0) break;
            int langEnd = text.indexOf('\n', open + 3);
            if (langEnd < 0) break;
            String language = text.substring(open + 3, langEnd).trim();
            int close = text.indexOf("```", langEnd + 1);
            if (close < 0) {
                String body = text.substring(langEnd + 1).trim();
                if (!body.isEmpty()) fences.add(new Fence(language, body));
                break;
            }
            String body = text.substring(langEnd + 1, close).trim();
            if (!body.isEmpty()) fences.add(new Fence(language, body));
            cursor = close + 3;
        }
        return fences;
    }

    public static List<String> bodies(String text) {
        List<Fence> fences = all(text);
        List<String> bodies = new ArrayList<>(fences.size());
        for (Fence fence : fences) bodies.add(fence.getBody());
        return Collections.unmodifiableList(bodies);
    }

    /** Last fenced body, or the whole text if there is no fence (legacy Save-as-file behavior). */
    public static String lastFenceOrBody(String text) {
        if (text == null) return "";
        List<Fence> fences = all(text);
        if (fences.isEmpty()) return text.trim();
        return fences.get(fences.size() - 1).getBody();
    }

    public static String lastFence(String text) {
        List<Fence> fences = all(text);
        if (fences.isEmpty()) return "";
        return fences.get(fences.size() - 1).getBody();
    }

    public static int count(String text) {
        return all(text).size();
    }
}
