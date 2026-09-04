package com.kairo.app.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** User-editable prompt templates stored on-device. */
public final class PromptTemplateStore {
    private static final String PREFS = "kairo_prompt_templates";
    private final SharedPreferences preferences;

    public PromptTemplateStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (preferences.getAll().isEmpty()) {
            seedDefaults();
        }
    }

    private void seedDefaults() {
        preferences.edit()
                .putString("Explain code", "Explain the following code step by step, including edge cases:\n\n")
                .putString("Write tests", "Write focused unit tests for the following code:\n\n")
                .putString("Security review", "Review for security issues and suggest safer alternatives:\n\n")
                .putString("Dev Loop task", "Run a full Dev Loop (Plan→Code→Test→Review→Edit→Debug). Task:\n\n")
                .apply();
    }

    public List<String> names() {
        List<String> names = new ArrayList<>(preferences.getAll().keySet());
        Collections.sort(names);
        return names;
    }

    public String get(String name) {
        return preferences.getString(name, "");
    }

    public void save(String name, String body) {
        if (name == null || name.trim().isEmpty()) return;
        preferences.edit().putString(name.trim(), body == null ? "" : body).apply();
    }

    public void delete(String name) {
        preferences.edit().remove(name).apply();
    }

    public String exportAll() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> e : preferences.getAll().entrySet()) {
            sb.append("## ").append(e.getKey()).append("\n")
                    .append(String.valueOf(e.getValue())).append("\n\n");
        }
        return sb.toString();
    }
}
