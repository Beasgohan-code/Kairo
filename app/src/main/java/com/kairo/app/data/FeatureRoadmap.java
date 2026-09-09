package com.kairo.app.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Suggested next features. Shown in-app so the product stays honest about
 * what is shipped versus what would be valuable later.
 */
public final class FeatureRoadmap {
    public static final class Idea {
        private final String title;
        private final String why;
        private final String effort;

        public Idea(String title, String why, String effort) {
            this.title = title;
            this.why = why;
            this.effort = effort;
        }

        public String getTitle() {
            return title;
        }

        public String getWhy() {
            return why;
        }

        public String getEffort() {
            return effort;
        }

        public String line() {
            return title + "  ·  " + effort + "\n" + why;
        }
    }

    private static final List<Idea> IDEAS = Collections.unmodifiableList(Arrays.asList(
            new Idea("Streaming tool-calling with confirm gates",
                    "Let the model propose a ToolRegistry action, then require the same visible confirmation used today.",
                    "L"),
            new Idea("Biometric app lock",
                    "Upgrade the PIN gate to AndroidX Biometric while keeping a PIN fallback.",
                    "M"),
            new Idea("Conversation folders and tags",
                    "Group threads by project so the sidebar stays usable past a few dozen chats.",
                    "M"),
            new Idea("Sandbox RAG",
                    "Keyword+chunk retrieval over private artifacts injected only after you pick the hits.",
                    "L"),
            new Idea("Cost estimator",
                    "Show a rough $ range from token estimates and a per-provider price table the user can edit.",
                    "M"),
            new Idea("Multi-account provider profiles",
                    "Switch Experiential/OpenRouter keys per project without deleting the other.",
                    "M"),
            new Idea("Skill sharing / import",
                    "Export a reviewed custom skill as text and import it on another device, still permissionless.",
                    "S"),
            new Idea("Tablet split pane",
                    "Chat + artifacts or Chat + Arena side by side on large screens.",
                    "L"),
            new Idea("i18n / translations",
                    "Ship UI strings through resources so the workspace can follow device locale.",
                    "L"),
            new Idea("MCP / local tool servers",
                    "Opt-in connector to a user-run MCP endpoint, still confirmation-gated for writes.",
                    "L"),
            new Idea("Export to Gist after confirm",
                    "One GitHub Contents/Gist path that always shows the file list first.",
                    "S"),
            new Idea("Branch conversations",
                    "Fork a thread at any assistant turn without losing the original.",
                    "M"),
            new Idea("Material You dynamic color",
                    "Optional wallpaper-based accents on top of Dark/Light/System.",
                    "S"),
            new Idea("Wear / widget reply snippet",
                    "Home widget that shows the last answer and opens the matching thread.",
                    "M"),
            new Idea("Prompt cache / reuse badges",
                    "Surface when a provider is likely caching the system prefix to save tokens.",
                    "S")
    ));

    private FeatureRoadmap() {
    }

    public static List<Idea> all() {
        return IDEAS;
    }

    public static String asText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ideas for later (not promises)\n\n");
        for (Idea idea : IDEAS) {
            sb.append("• ").append(idea.getTitle()).append(" (").append(idea.getEffort()).append(")\n  ")
                    .append(idea.getWhy()).append("\n\n");
        }
        sb.append("S/M/L is relative effort. Writes would stay review-first.");
        return sb.toString().trim();
    }
}
