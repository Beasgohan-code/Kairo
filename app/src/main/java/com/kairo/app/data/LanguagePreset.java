package com.kairo.app.data;

/** A code-language choice shared by artifact creation and the coding prompt. */
public final class LanguagePreset {
    private final String id;
    private final String label;
    private final String extension;
    private final String description;

    public LanguagePreset(String id, String label, String extension, String description) {
        this.id = id;
        this.label = label;
        this.extension = extension;
        this.description = description;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public String getExtension() { return extension; }
    public String getDescription() { return description; }
}
