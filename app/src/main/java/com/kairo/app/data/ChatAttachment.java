package com.kairo.app.data;

/** A bounded inline image attachment for providers that support multimodal chat. */
public final class ChatAttachment {
    private final String name;
    private final String mimeType;
    private final String base64;

    public ChatAttachment(String name, String mimeType, String base64) {
        this.name = name == null || name.trim().isEmpty() ? "image" : name.trim();
        this.mimeType = mimeType == null || mimeType.trim().isEmpty() ? "image/jpeg" : mimeType.trim();
        this.base64 = base64 == null ? "" : base64;
    }

    public String getName() { return name; }
    public String getMimeType() { return mimeType; }
    public String getBase64() { return base64; }

    public String displayLabel() {
        return name + "  ·  " + mimeType;
    }
}
