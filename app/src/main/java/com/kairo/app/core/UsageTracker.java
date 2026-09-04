package com.kairo.app.core;

import android.content.Context;
import android.content.SharedPreferences;

/** Local-only, approximate activity counters for a useful Settings health panel. */
public final class UsageTracker {
    private static final String PREFS = "kairo_usage";
    private final SharedPreferences preferences;

    public UsageTracker(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void recordMessage() {
        preferences.edit()
                .putInt("messages", preferences.getInt("messages", 0) + 1)
                .putLong("lastMessage", System.currentTimeMillis())
                .apply();
    }

    public int messageCount() {
        return preferences.getInt("messages", 0);
    }

    public long lastMessageAt() {
        return preferences.getLong("lastMessage", 0L);
    }
}
