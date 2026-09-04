package com.kairo.app.data;

import java.util.ArrayList;
import java.util.List;

/** A saved local chat thread. Prompt content never leaves the device unless sent by the user. */
public final class ConversationSession {
    private final String id;
    private String title;
    private long updatedAt;
    private final List<ChatMessage> messages;

    public ConversationSession(String id, String title, long updatedAt, List<ChatMessage> messages) {
        this.id = id;
        this.title = title == null || title.trim().isEmpty() ? "New conversation" : title;
        this.updatedAt = updatedAt;
        this.messages = new ArrayList<>();
        if (messages != null) this.messages.addAll(messages);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title != null && !title.trim().isEmpty()) this.title = title.trim();
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        updatedAt = System.currentTimeMillis();
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
