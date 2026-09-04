package com.kairo.app.data;

/** Describes a service that can be connected to Kairo's explicit tools. */
public final class ConnectorDefinition {
    private final String id;
    private final String name;
    private final String eyebrow;
    private final String description;
    private final String capabilities;
    private final boolean requiresApiKey;
    private final boolean supportsWrites;

    public ConnectorDefinition(
            String id,
            String name,
            String eyebrow,
            String description,
            String capabilities,
            boolean requiresApiKey,
            boolean supportsWrites) {
        this.id = id;
        this.name = name;
        this.eyebrow = eyebrow;
        this.description = description;
        this.capabilities = capabilities;
        this.requiresApiKey = requiresApiKey;
        this.supportsWrites = supportsWrites;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEyebrow() { return eyebrow; }
    public String getDescription() { return description; }
    public String getCapabilities() { return capabilities; }
    public boolean requiresApiKey() { return requiresApiKey; }
    public boolean supportsWrites() { return supportsWrites; }
}
