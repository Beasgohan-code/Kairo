package com.kairo.app.core;

import com.kairo.app.data.ModelInfo;

/** Endpoint and key metadata shared by the chat and model discovery clients. */
public final class ProviderConfig {
    private ProviderConfig() {
    }

    public static String displayName(String providerId) {
        if ("openrouter".equals(providerId)) return "OpenRouter";
        if ("groq".equals(providerId)) return "Groq";
        if ("moonshot".equals(providerId)) return "Kimi / Moonshot";
        if ("nvidia".equals(providerId)) return "NVIDIA NIM";
        if ("mistral".equals(providerId)) return "Mistral AI";
        if ("anthropic".equals(providerId)) return "Anthropic";
        if ("openai".equals(providerId)) return "OpenAI";
        if ("ollama".equals(providerId)) return "Ollama local";
        if ("github".equals(providerId)) return "GitHub";
        if ("brave".equals(providerId)) return "Brave Search";
        if ("vercel".equals(providerId)) return "Vercel";
        if ("n8n".equals(providerId)) return "n8n";
        if ("slack".equals(providerId)) return "Slack";
        if ("notion".equals(providerId)) return "Notion";
        if ("supabase".equals(providerId)) return "Supabase";
        if ("discord".equals(providerId)) return "Discord webhook";
        if ("linear".equals(providerId)) return "Linear";
        if ("custom".equals(providerId)) return "OpenAI-compatible";
        return providerId;
    }

    public static String apiKeyHint(String providerId) {
        if ("nvidia".equals(providerId)) return "nvapi-…";
        if ("moonshot".equals(providerId)) return "Kimi / Moonshot API key";
        if ("mistral".equals(providerId)) return "Mistral API key";
        if ("groq".equals(providerId)) return "gsk_…";
        if ("openrouter".equals(providerId)) return "sk-or-v1-…";
        if ("anthropic".equals(providerId)) return "sk-ant-…";
        if ("github".equals(providerId)) return "ghp_… or fine-grained token";
        if ("brave".equals(providerId)) return "Brave subscription token";
        if ("vercel".equals(providerId)) return "Vercel bearer token";
        if ("n8n".equals(providerId)) return "n8n API key";
        if ("slack".equals(providerId)) return "xoxb-… bot token";
        if ("notion".equals(providerId)) return "ntn_… integration token";
        if ("supabase".equals(providerId)) return "sb_publishable_… or scoped key";
        if ("discord".equals(providerId)) return "https://discord.com/api/webhooks/…";
        if ("linear".equals(providerId)) return "Linear API key";
        return "Paste provider key";
    }

    public static String baseUrl(String providerId, AppPreferences preferences) {
        if ("openrouter".equals(providerId)) return "https://openrouter.ai/api/v1";
        if ("groq".equals(providerId)) return "https://api.groq.com/openai/v1";
        if ("moonshot".equals(providerId)) return "https://api.moonshot.ai/v1";
        if ("nvidia".equals(providerId)) return "https://integrate.api.nvidia.com/v1";
        if ("mistral".equals(providerId)) return "https://api.mistral.ai/v1";
        if ("openai".equals(providerId)) return "https://api.openai.com/v1";
        if ("anthropic".equals(providerId)) return "https://api.anthropic.com";
        if ("ollama".equals(providerId)) return preferences.getOllamaBaseUrl();
        if ("custom".equals(providerId)) return preferences.getCustomBaseUrl();
        return "";
    }

    public static boolean usesAnthropicApi(String providerId) {
        return "anthropic".equals(providerId);
    }

    public static boolean needsApiKey(String providerId) {
        return !"ollama".equals(providerId);
    }

    public static String requestModelId(ModelInfo model) {
        return model == null ? "" : model.getId();
    }
}
