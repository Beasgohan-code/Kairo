package com.kairo.app.data;

import java.util.Locale;

/** A model that can be selected in Kairo's provider-neutral chat UI. */
public final class ModelInfo {
    private final String id;
    private final String name;
    private final String providerId;
    private final String description;
    private final boolean freeRoute;
    private final boolean local;
    private final String contextWindow;
    private final String billingNote;
    private final boolean candidate;

    public ModelInfo(
            String id,
            String name,
            String providerId,
            String description,
            boolean freeRoute,
            boolean local,
            String contextWindow,
            String billingNote,
            boolean candidate) {
        this.id = id;
        this.name = name;
        this.providerId = providerId;
        this.description = description;
        this.freeRoute = freeRoute;
        this.local = local;
        this.contextWindow = contextWindow;
        this.billingNote = billingNote;
        this.candidate = candidate;
    }

    public ModelInfo(
            String id,
            String name,
            String providerId,
            String description,
            boolean freeRoute,
            boolean local,
            String contextWindow,
            String billingNote) {
        this(id, name, providerId, description, freeRoute, local, contextWindow, billingNote, false);
    }

    public boolean isCandidate() {
        return candidate;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getDescription() {
        return description;
    }

    /** True means the route is advertised as free or commonly has a free tier. */
    public boolean isFreeRoute() {
        return freeRoute;
    }

    public boolean isLocal() {
        return local;
    }

    public String getContextWindow() {
        return contextWindow;
    }

    public String getBillingNote() {
        return billingNote;
    }

    public boolean matches(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.US);
        return id.toLowerCase(Locale.US).contains(needle)
                || name.toLowerCase(Locale.US).contains(needle)
                || providerId.toLowerCase(Locale.US).contains(needle)
                || description.toLowerCase(Locale.US).contains(needle);
    }

    @Override
    public String toString() {
        return name + " (" + providerId + ")";
    }
}
