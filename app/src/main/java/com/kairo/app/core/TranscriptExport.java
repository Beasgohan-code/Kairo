package com.kairo.app.core;

import com.kairo.app.data.ChatMessage;

import java.util.List;

/** Redacted Markdown / JSON conversation export. Credentials are stripped before sharing. */
public final class TranscriptExport {
    private TranscriptExport() {
    }

    public static String markdown(String title, List<ChatMessage> messages) {
        return markdown(title, messages, "");
    }

    public static String markdown(String title, List<ChatMessage> messages, String projectInstructions) {
        StringBuilder md = new StringBuilder();
        String heading = title == null || title.trim().isEmpty() ? "Conversation export" : title.trim();
        md.append("# ").append(ApiKeyDetector.redact(heading)).append("\n\n");
        md.append("_Redacted export · credentials stripped · private_\n\n");
        if (messages != null) {
            for (ChatMessage message : messages) {
                if (message == null) continue;
                String role = "user".equals(message.getRole()) ? "You" : "Assistant";
                md.append("### ").append(role).append("\n\n");
                md.append(ApiKeyDetector.redact(message.getContent() == null ? "" : message.getContent()));
                md.append("\n\n");
            }
        }
        if (projectInstructions != null && !projectInstructions.trim().isEmpty()) {
            md.append("---\n\n### Project instructions\n\n")
                    .append(ApiKeyDetector.redact(projectInstructions.trim()))
                    .append('\n');
        }
        return md.toString();
    }

    public static String json(String title, List<ChatMessage> messages) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"title\": ").append(quote(ApiKeyDetector.redact(title == null ? "" : title))).append(",\n");
        json.append("  \"redacted\": true,\n");
        json.append("  \"messages\": [\n");
        if (messages != null) {
            boolean first = true;
            for (ChatMessage message : messages) {
                if (message == null) continue;
                if (!first) json.append(",\n");
                first = false;
                json.append("    {\"role\": ").append(quote(message.getRole() == null ? "user" : message.getRole()))
                        .append(", \"content\": ")
                        .append(quote(ApiKeyDetector.redact(message.getContent() == null ? "" : message.getContent())))
                        .append('}');
            }
            if (!first) json.append('\n');
        }
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    public static String sharePlain(String title, List<ChatMessage> messages) {
        StringBuilder transcript = new StringBuilder();
        if (title != null && !title.trim().isEmpty()) {
            transcript.append(ApiKeyDetector.redact(title.trim())).append("\n\n");
        }
        if (messages != null) {
            for (ChatMessage message : messages) {
                if (message == null) continue;
                transcript.append("user".equals(message.getRole()) ? "You" : "Kairo")
                        .append(":\n")
                        .append(ApiKeyDetector.redact(message.getContent() == null ? "" : message.getContent()))
                        .append("\n\n");
            }
        }
        return transcript.toString();
    }

    private static String quote(String value) {
        String raw = value == null ? "" : value;
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' || c == '"') out.append('\\').append(c);
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else out.append(c);
        }
        out.append('"');
        return out.toString();
    }
}
