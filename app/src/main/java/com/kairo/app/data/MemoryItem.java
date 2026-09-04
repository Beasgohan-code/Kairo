package com.kairo.app.data;

/** A user-approved, device-local memory that can be included as context in a later chat. */
public final class MemoryItem {
    private final String id;
    private final String category;
    private final String content;
    private final long createdAt;
    private long updatedAt;

    public MemoryItem(String id, String category, String content, long createdAt, long updatedAt) {
        this.id = id == null ? "" : id;
        this.category = category == null || category.trim().isEmpty() ? "note" : category.trim();
        this.content = content == null ? "" : content.trim();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        updatedAt = System.currentTimeMillis();
    }
}
