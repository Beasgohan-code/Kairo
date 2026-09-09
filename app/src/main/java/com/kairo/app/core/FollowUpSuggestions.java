package com.kairo.app.core;

import java.util.Locale;

/** Heuristic follow-up chips shown under an assistant answer. */
public final class FollowUpSuggestions {
    private FollowUpSuggestions() {
    }

    public static String[] forAnswer(String answer) {
        String lower = answer == null ? "" : answer.toLowerCase(Locale.US);
        if (lower.contains("```") || lower.contains("function") || lower.contains("class ")
                || lower.contains("def ") || lower.contains("public static")) {
            return new String[]{
                    "Explain this code step by step",
                    "Find potential bugs or edge cases",
                    "Convert this to TypeScript"
            };
        }
        if (lower.contains("error") || lower.contains("exception") || lower.contains("fail")
                || lower.contains("traceback") || lower.contains("stack trace")) {
            return new String[]{
                    "How do I fix this?",
                    "Show a minimal reproducible example",
                    "What are safer alternatives?"
            };
        }
        if (lower.length() > 600) {
            return new String[]{
                    "Summarize the key points",
                    "Make this more concise",
                    "Turn this into action items"
            };
        }
        return new String[]{
                "Go deeper on this",
                "Give a practical example",
                "What should I do next?"
        };
    }
}
