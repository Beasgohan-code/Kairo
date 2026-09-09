package com.kairo.app.core;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Maps a spoken utterance to a workspace command using phrase / word-boundary matching.
 * Greedy {@code contains("files")} matching is avoided so dictation is not hijacked.
 */
public final class VoiceCommandRouter {
    public enum Action {
        NEW_CHAT,
        SETTINGS,
        MODELS,
        ARENA,
        SANDBOX,
        ARTIFACTS,
        CREATE_FILE,
        IMAGE_STUDIO,
        CAMERA,
        WEB_SEARCH,
        THEME_DARK,
        THEME_LIGHT,
        THEME_SYSTEM,
        FAST,
        DEEP,
        BALANCED,
        GROQ,
        ASTRA,
        CLAUDE,
        SEND,
        DICTATE
    }

    public static final class Result {
        private final Action action;
        private final String dictated;

        private Result(Action action, String dictated) {
            this.action = action;
            this.dictated = dictated == null ? "" : dictated;
        }

        public Action getAction() {
            return action;
        }

        public String getDictated() {
            return dictated;
        }

        public boolean isDictate() {
            return action == Action.DICTATE;
        }
    }

    private VoiceCommandRouter() {
    }

    public static Result route(String spoken) {
        if (spoken == null) return new Result(Action.DICTATE, "");
        String raw = spoken.trim();
        if (raw.isEmpty()) return new Result(Action.DICTATE, "");
        String lower = raw.toLowerCase(Locale.US).replaceAll("\\s+", " ");

        if (phrase(lower, "new chat", "start over", "clear chat")) {
            return new Result(Action.NEW_CHAT, raw);
        }
        if (command(lower, "settings") || phrase(lower, "open settings")) {
            return new Result(Action.SETTINGS, raw);
        }
        if (phrase(lower, "open models", "show models", "model picker")) {
            return new Result(Action.MODELS, raw);
        }
        if (phrase(lower, "open arena", "model arena", "compare models")) {
            return new Result(Action.ARENA, raw);
        }
        if (phrase(lower, "open sandbox", "sandbox terminal") || command(lower, "terminal", "ubuntu")) {
            return new Result(Action.SANDBOX, raw);
        }
        if (phrase(lower, "create file", "new file", "quick template")) {
            return new Result(Action.CREATE_FILE, raw);
        }
        if (phrase(lower, "open artifacts", "open files", "show artifacts", "show files")) {
            return new Result(Action.ARTIFACTS, raw);
        }
        if (phrase(lower, "image studio", "generate image", "make image")) {
            return new Result(Action.IMAGE_STUDIO, raw);
        }
        if (phrase(lower, "take photo", "open camera") || command(lower, "camera")) {
            return new Result(Action.CAMERA, raw);
        }
        if (phrase(lower, "web search", "search the web")) {
            return new Result(Action.WEB_SEARCH, raw);
        }
        if (phrase(lower, "dark mode", "dark theme")) {
            return new Result(Action.THEME_DARK, raw);
        }
        if (phrase(lower, "light mode", "light theme")) {
            return new Result(Action.THEME_LIGHT, raw);
        }
        if (phrase(lower, "system theme", "system mode")) {
            return new Result(Action.THEME_SYSTEM, raw);
        }
        if (phrase(lower, "fast mode", "fast reasoning")) {
            return new Result(Action.FAST, raw);
        }
        if (phrase(lower, "deep mode", "deep reasoning", "think hard")) {
            return new Result(Action.DEEP, raw);
        }
        if (phrase(lower, "balanced mode", "balanced reasoning")) {
            return new Result(Action.BALANCED, raw);
        }
        if (phrase(lower, "use groq", "switch to groq")) {
            return new Result(Action.GROQ, raw);
        }
        if (phrase(lower, "use astra", "gpt 6", "gpt-6", "experiential")) {
            return new Result(Action.ASTRA, raw);
        }
        if (phrase(lower, "use claude") || command(lower, "anthropic")) {
            return new Result(Action.CLAUDE, raw);
        }
        if ((command(lower, "send") || command(lower, "go")) && lower.length() <= 8) {
            return new Result(Action.SEND, raw);
        }
        return new Result(Action.DICTATE, raw);
    }

    static boolean phrase(String lower, String... phrases) {
        for (String phrase : phrases) {
            if (lower.equals(phrase) || lower.startsWith(phrase + " ") || lower.endsWith(" " + phrase)
                    || lower.contains(" " + phrase + " ") || lower.contains(" " + phrase + ".")
                    || lower.contains(" " + phrase + ",")) {
                return true;
            }
        }
        return false;
    }

    static boolean command(String lower, String... words) {
        for (String word : words) {
            if (word == null || word.isEmpty()) continue;
            if (lower.equals(word) || lower.equals("open " + word) || lower.equals("show " + word)) {
                return true;
            }
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
            // Single-word commands only fire as the whole utterance or an "open/show X" phrase.
            if (lower.startsWith("open ") || lower.startsWith("show ")) {
                if (pattern.matcher(lower).find()) return true;
            }
        }
        return false;
    }
}
