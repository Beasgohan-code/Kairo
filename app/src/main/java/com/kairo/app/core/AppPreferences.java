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
    private static final String KEY_THEME_MODE = "ui_theme_mode"; // "dark" | "light" | "system"
    private static final String KEY_SYSTEM_INSTRUCTIONS = "system_project_instructions";
    private static final String KEY_VOICE_CONTINUOUS = "voice_continuous_mode";
    private static final String KEY_APP_LOCK = "security_app_lock_enabled";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_LARGE_TEXT = "accessibility_large_text";
    private static final String KEY_PINNED_SESSIONS = "pinned_session_ids";
    private static final String KEY_COMPOSER_DRAFT = "composer_draft";
    private static final String KEY_PIN_HASH = "security_pin_hash";
    private static final String KEY_PIN_SALT = "security_pin_salt";
    private static final String KEY_WHATS_NEW_SEEN = "whats_new_seen_version";
    private static final String SKILL_SEPARATOR = "\u001f";

    private final SharedPreferences preferences;

    public AppPreferences(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getProvider() {
        return preferences.getString(KEY_PROVIDER, "experiential");
    }

    public String getModel() {
        return preferences.getString(KEY_MODEL, "gpt-6-astra");
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

    public String getThemeMode() {
        return preferences.getString(KEY_THEME_MODE, "dark");
    }

    /** Resolved light appearance (system follows Configuration.UI_MODE_NIGHT). */
    public boolean isLightTheme() {
        return isLightTheme(null);
    }

    public boolean isLightTheme(android.content.Context context) {
        String mode = getThemeMode();
        if ("light".equals(mode)) return true;
        if ("dark".equals(mode)) return false;
        // system
        if (context == null) return false;
        int night = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return night != android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public void setThemeMode(String themeMode) {
        String raw = themeMode == null ? "dark" : themeMode.trim().toLowerCase(java.util.Locale.US);
        String value = "light".equals(raw) || "system".equals(raw) ? raw : "dark";
        preferences.edit().putString(KEY_THEME_MODE, value).apply();
    }

    public String themeModeLabel() {
        String mode = getThemeMode();
        if ("light".equals(mode)) return "Light";
        if ("system".equals(mode)) return "System";
        return "Dark";
    }


    public String getSystemInstructions() {
        return preferences.getString(KEY_SYSTEM_INSTRUCTIONS, "");
    }

    public void setSystemInstructions(String value) {
        preferences.edit().putString(KEY_SYSTEM_INSTRUCTIONS, value == null ? "" : value.trim()).apply();
    }

    public boolean isVoiceContinuous() {
        return preferences.getBoolean(KEY_VOICE_CONTINUOUS, false);
    }

    public void setVoiceContinuous(boolean enabled) {
        preferences.edit().putBoolean(KEY_VOICE_CONTINUOUS, enabled).apply();
    }

    public boolean isAppLockEnabled() {
        return preferences.getBoolean(KEY_APP_LOCK, false);
    }

    public void setAppLockEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_APP_LOCK, enabled).apply();
    }


    public boolean isOnboardingDone() {
        return preferences.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean done) {
        preferences.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    public boolean isLargeText() {
        return preferences.getBoolean(KEY_LARGE_TEXT, false);
    }

    public void setLargeText(boolean enabled) {
        preferences.edit().putBoolean(KEY_LARGE_TEXT, enabled).apply();
    }

    public java.util.Set<String> getPinnedSessionIds() {
        String raw = preferences.getString(KEY_PINNED_SESSIONS, "");
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        if (raw != null && !raw.isEmpty()) {
            for (String p : raw.split("\u001f")) {
                if (!p.isEmpty()) set.add(p);
            }
        }
        return set;
    }

    public void setPinnedSessionIds(java.util.Set<String> ids) {
        StringBuilder sb = new StringBuilder();
        if (ids != null) {
            for (String id : ids) {
                if (id == null || id.isEmpty()) continue;
                if (sb.length() > 0) sb.append('\u001f');
                sb.append(id);
            }
        }
        preferences.edit().putString(KEY_PINNED_SESSIONS, sb.toString()).apply();
    }

    public void togglePinnedSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return;
        java.util.Set<String> set = getPinnedSessionIds();
        if (set.contains(sessionId)) set.remove(sessionId);
        else set.add(sessionId);
        setPinnedSessionIds(set);
    }

    public String getComposerDraft() {
        return preferences.getString(KEY_COMPOSER_DRAFT, "");
    }

    public void setComposerDraft(String draft) {
        String value = draft == null ? "" : draft;
        if (value.length() > 32_000) value = value.substring(0, 32_000);
        preferences.edit().putString(KEY_COMPOSER_DRAFT, value).apply();
    }

    public boolean hasPin() {
        String hash = preferences.getString(KEY_PIN_HASH, "");
        String salt = preferences.getString(KEY_PIN_SALT, "");
        return hash != null && !hash.isEmpty() && salt != null && !salt.isEmpty();
    }

    public boolean setPin(String pin) {
        if (!PinHasher.isValidPin(pin)) return false;
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash(pin, salt);
        if (hash.isEmpty()) return false;
        preferences.edit()
                .putString(KEY_PIN_SALT, salt)
                .putString(KEY_PIN_HASH, hash)
                .apply();
        return true;
    }

    public boolean verifyPin(String pin) {
        return PinHasher.verify(pin,
                preferences.getString(KEY_PIN_SALT, ""),
                preferences.getString(KEY_PIN_HASH, ""));
    }

    public void clearPin() {
        preferences.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT).apply();
    }

    public String getWhatsNewSeen() {
        return preferences.getString(KEY_WHATS_NEW_SEEN, "");
    }

    public void setWhatsNewSeen(String version) {
        preferences.edit().putString(KEY_WHATS_NEW_SEEN, version == null ? "" : version).apply();
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
