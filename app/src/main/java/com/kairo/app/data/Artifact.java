package com.kairo.app.data;

import java.util.Locale;

/** Metadata for a file created inside Kairo's private artifact workspace. */
public final class Artifact {
    private final String id;
    private String name;
    private String language;
    private long updatedAt;
    private final String content;

    public Artifact(String id, String name, String language, long updatedAt, String content) {
        this.id = id;
        this.name = name == null || name.trim().isEmpty() ? "untitled.txt" : name.trim();
        this.language = language == null || language.trim().isEmpty() ? inferLanguage(this.name) : language.trim();
        this.updatedAt = updatedAt;
        this.content = content == null ? "" : content;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLanguage() { return language; }
    public long getUpdatedAt() { return updatedAt; }
    public String getContent() { return content; }

    public void touch() { updatedAt = System.currentTimeMillis(); }

    public static String inferLanguage(String name) {
        if (name == null) return "text";
        String lower = name.toLowerCase(Locale.US);
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".kt")) return "kotlin";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".ts")) return "typescript";
        if (lower.endsWith(".sh") || lower.endsWith(".bash")) return "shell";
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".gradle")) return "groovy";
        if (lower.endsWith(".html")) return "html";
        if (lower.endsWith(".css")) return "css";
        return "text";
    }
}
