package com.kairo.app.data;

/** Metadata used by the agent workspace. Tools are intentionally explicit. */
public final class AgentDefinition {
    private final String id;
    private final String name;
    private final String eyebrow;
    private final String description;
    private final String capabilities;
    private final boolean requiresNetwork;

    public AgentDefinition(
            String id,
            String name,
            String eyebrow,
            String description,
            String capabilities,
            boolean requiresNetwork) {
        this.id = id;
        this.name = name;
        this.eyebrow = eyebrow;
        this.description = description;
        this.capabilities = capabilities;
        this.requiresNetwork = requiresNetwork;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEyebrow() {
        return eyebrow;
    }

    public String getDescription() {
        return description;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public boolean requiresNetwork() {
        return requiresNetwork;
    }
}
