package com.kairo.app.data;

/** A deterministic workspace behavior that can be enabled without granting a tool permission. */
public final class SkillDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final String instruction;
    private final boolean enabledByDefault;

    public SkillDefinition(String id, String name, String description, String instruction,
                           boolean enabledByDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.instruction = instruction;
        this.enabledByDefault = enabledByDefault;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getInstruction() { return instruction; }
    public boolean isEnabledByDefault() { return enabledByDefault; }
}
