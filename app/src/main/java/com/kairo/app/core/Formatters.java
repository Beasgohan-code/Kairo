package com.kairo.app.core;

import java.util.Locale;

public final class Formatters {
    private Formatters() {
    }

    public static String compactCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format(Locale.US, "%.1fk", count / 1000f);
        return String.format(Locale.US, "%.1fM", count / 1000000f);
    }

    public static String providerLabel(String provider) {
        return ProviderConfig.displayName(provider);
    }
}
