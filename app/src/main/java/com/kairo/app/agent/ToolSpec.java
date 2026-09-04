package com.kairo.app.agent;

/** Metadata shown to users and suitable for an LLM tool schema. No tool is implicit. */
public final class ToolSpec {
    private final String name;
    private final String label;
    private final String description;
    private final boolean writes;
    private final boolean network;

    public ToolSpec(String name, String label, String description, boolean writes, boolean network) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.writes = writes;
        this.network = network;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isWriteTool() {
        return writes;
    }

    public boolean usesNetwork() {
        return network;
    }
}
