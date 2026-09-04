package com.kairo.app.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.kairo.app.data.SkillCatalog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Non-sensitive UI preferences. Secrets belong in ApiKeyStore. */
public final class AppPreferences {
    private static final String PREFS = "kairo_preferences";
    private static final String KEY_PROVIDER = "selected_provider";
    private static final String KEY_MODEL = "selected_model";
    private static final String KEY_BASE_URL = "openai_compatible_base_url";
    private static final String KEY_OLLAMA_URL = "ollama_base_url";
    private static final String KEY_VERCEL_URL = "vercel_base_url";
    private static final String KEY_VERCEL_TEAM = "vercel_team_id";
    private static final String KEY_VERCEL_PROJECT = "vercel_project";
    private static final String KEY_N8N_URL = "n8n_base_url";
    private static final String KEY_N8N_WEBHOOK = "n8n_webhook_url";
    private static final String KEY_SUPABASE_URL = "supabase_url";
    private static final String KEY_SUPABASE_TABLE = "supabase_table";
    private static final String KEY_LANGUAGE_PRESET = "language_preset";
    private static final String KEY_ENABLED_SKILLS = "enabled_skills";
    private static final String KEY_TEMPERATURE = "generation_temperature";
    private static final String KEY_MAX_OUTPUT_TOKENS = "generation_max_output_tokens";
    private static final String KEY_RESPONSE_STYLE = "generation_response_style";
    private static final String KEY_REASONING_MODE = "generation_reasoning_mode";
    private static final String SKILL_SEPARATOR = "\u001f";

    private final SharedPreferences preferences;

    public AppPreferences(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getProvider() {
        return preferences.getString(KEY_PROVIDER, "openrouter");
    }

    public String getModel() {
        return preferences.getString(KEY_MODEL, "deepseek/deepseek-r1:free");
    }

    public void setModel(String provider, String model) {
        preferences.edit()
                .putString(KEY_PROVIDER, provider)
                .putString(KEY_MODEL, model)
                .apply();
    }

    public String getCustomBaseUrl() {
        return preferences.getString(KEY_BASE_URL, "https://api.openai.com/v1");
    }

    public void setCustomBaseUrl(String url) {
        preferences.edit().putString(KEY_BASE_URL, normalizeUrl(url)).apply();
    }

    public String getOllamaBaseUrl() {
        return preferences.getString(KEY_OLLAMA_URL, "http://10.0.2.2:11434");
    }

    public void setOllamaBaseUrl(String url) {
        preferences.edit().putString(KEY_OLLAMA_URL, normalizeUrl(url)).apply();
    }

    public String getVercelBaseUrl() {
        return preferences.getString(KEY_VERCEL_URL, "https://api.vercel.com");
    }

    public void setVercelBaseUrl(String url) {
        String normalized = normalizeUrl(url);
        preferences.edit().putString(KEY_VERCEL_URL,
                normalized.isEmpty() ? "https://api.vercel.com" : normalized).apply();
    }

    public String getVercelTeamId() {
        return preferences.getString(KEY_VERCEL_TEAM, "");
    }

    public void setVercelTeamId(String teamId) {
        preferences.edit().putString(KEY_VERCEL_TEAM, teamId == null ? "" : teamId.trim()).apply();
    }

    public String getVercelProject() {
        return preferences.getString(KEY_VERCEL_PROJECT, "");
    }

    public void setVercelProject(String project) {
        preferences.edit().putString(KEY_VERCEL_PROJECT, project == null ? "" : project.trim()).apply();
    }

    public String getN8nBaseUrl() {
        return preferences.getString(KEY_N8N_URL, "");
    }

    public void setN8nBaseUrl(String url) {
        preferences.edit().putString(KEY_N8N_URL, normalizeUrl(url)).apply();
    }

    public String getN8nWebhookUrl() {
        return preferences.getString(KEY_N8N_WEBHOOK, "");
    }

    public void setN8nWebhookUrl(String url) {
        preferences.edit().putString(KEY_N8N_WEBHOOK, normalizeUrl(url)).apply();
    }

    public String getSupabaseUrl() {
        return preferences.getString(KEY_SUPABASE_URL, "");
    }

    public void setSupabaseUrl(String url) {
        preferences.edit().putString(KEY_SUPABASE_URL, normalizeUrl(url)).apply();
    }

    public String getSupabaseTable() {
        return preferences.getString(KEY_SUPABASE_TABLE, "");
    }

    public void setSupabaseTable(String table) {
        preferences.edit().putString(KEY_SUPABASE_TABLE, table == null ? "" : table.trim()).apply();
    }

    public String getLanguagePreset() {
        return preferences.getString(KEY_LANGUAGE_PRESET, "auto");
    }

    public void setLanguagePreset(String languagePreset) {
        preferences.edit().putString(KEY_LANGUAGE_PRESET,
                languagePreset == null || languagePreset.trim().isEmpty() ? "auto" : languagePreset.trim()).apply();
    }

    /** Returns only known skill ids, preserving the catalog order and avoiding preference injection. */
    public List<String> getEnabledSkills() {
        String stored = preferences.getString(KEY_ENABLED_SKILLS, "");
        Set<String> requested = new LinkedHashSet<>();
        if (preferences.contains(KEY_ENABLED_SKILLS)) {
            if (stored != null && !stored.isEmpty()) {
                for (String value : stored.split(SKILL_SEPARATOR)) {
                    if (SkillCatalog.find(value) != null) requested.add(value);
                }
            }
        } else {
            requested.addAll(SkillCatalog.defaultIds());
        }
        List<String> result = new ArrayList<>();
        for (com.kairo.app.data.SkillDefinition skill : SkillCatalog.all()) {
            if (requested.contains(skill.getId())) result.add(skill.getId());
        }
        return result;
    }

    public void setEnabledSkills(List<String> skillIds) {
        Set<String> requested = new LinkedHashSet<>();
        if (skillIds != null) {
            for (String value : skillIds) {
                if (SkillCatalog.find(value) != null) requested.add(value);
            }
        }
        StringBuilder serialized = new StringBuilder();
        for (String value : requested) {
            if (serialized.length() > 0) serialized.append(SKILL_SEPARATOR);
            serialized.append(value);
        }
        preferences.edit().putString(KEY_ENABLED_SKILLS, serialized.toString()).apply();
    }

    public float getTemperature() {
        return preferences.getFloat(KEY_TEMPERATURE, 0.2f);
    }

    public void setTemperature(float temperature) {
        float safe = Math.max(0f, Math.min(2f, temperature));
        preferences.edit().putFloat(KEY_TEMPERATURE, safe).apply();
    }

    public int getMaxOutputTokens() {
        return preferences.getInt(KEY_MAX_OUTPUT_TOKENS, 2048);
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        int safe = Math.max(256, Math.min(8192, maxOutputTokens));
        preferences.edit().putInt(KEY_MAX_OUTPUT_TOKENS, safe).apply();
    }

    public String getResponseStyle() {
        return preferences.getString(KEY_RESPONSE_STYLE, "balanced");
    }

    public void setResponseStyle(String responseStyle) {
        String value = responseStyle == null ? "balanced" : responseStyle.trim().toLowerCase(java.util.Locale.US);
        if (!("concise".equals(value) || "balanced".equals(value) || "detailed".equals(value))) {
            value = "balanced";
        }
        preferences.edit().putString(KEY_RESPONSE_STYLE, value).apply();
    }

    public String getReasoningMode() {
        return preferences.getString(KEY_REASONING_MODE, "balanced");
    }

    public void setReasoningMode(String reasoningMode) {
        String value = reasoningMode == null ? "balanced" : reasoningMode.trim().toLowerCase(java.util.Locale.US);
        if (!("fast".equals(value) || "balanced".equals(value) || "deep".equals(value))) {
            value = "balanced";
        }
        preferences.edit().putString(KEY_REASONING_MODE, value).apply();
    }

    private String normalizeUrl(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
