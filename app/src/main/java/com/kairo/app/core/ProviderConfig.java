package com.kairo.app.core;

import com.kairo.app.data.ModelInfo;

/** Endpoint and key metadata shared by the chat and model discovery clients. */
public final class ProviderConfig {
    private ProviderConfig() {
    }

    public static String displayName(String providerId) {
        if ("experiential".equals(providerId)) return "Experiential Labs";
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
        if ("google".equals(providerId)) return "Google Gemini";
        if ("xai".equals(providerId)) return "xAI";
        if ("huggingface".equals(providerId)) return "Hugging Face";
        if ("perplexity".equals(providerId)) return "Perplexity";
        return providerId;
    }

    public static String apiKeyHint(String providerId) {
        if ("experiential".equals(providerId)) return "Experiential API key (Settings → API Keys)";
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
        if ("google".equals(providerId)) return "Google AI Studio key (AIza…)";
        if ("xai".equals(providerId)) return "xai-…";
        if ("huggingface".equals(providerId)) return "hf_…";
        if ("perplexity".equals(providerId)) return "pplx-…";
        return "Paste provider key";
    }

    public static String baseUrl(String providerId, AppPreferences preferences) {
        // OpenAI-compatible Chat Completions. Keys from platform.experientiallabs.ai.
        if ("experiential".equals(providerId)) return "https://api.experientiallabs.ai/v1";
        if ("openrouter".equals(providerId)) return "https://openrouter.ai/api/v1";
        if ("groq".equals(providerId)) return "https://api.groq.com/openai/v1";
        if ("moonshot".equals(providerId)) return "https://api.moonshot.ai/v1";
        if ("nvidia".equals(providerId)) return "https://integrate.api.nvidia.com/v1";
        if ("mistral".equals(providerId)) return "https://api.mistral.ai/v1";
        if ("openai".equals(providerId)) return "https://api.openai.com/v1";
        if ("anthropic".equals(providerId)) return "https://api.anthropic.com";
        if ("google".equals(providerId)) return "https://generativelanguage.googleapis.com/v1beta/openai";
        if ("xai".equals(providerId)) return "https://api.x.ai/v1";
        if ("huggingface".equals(providerId)) return "https://router.huggingface.co/v1";
        if ("perplexity".equals(providerId)) return "https://api.perplexity.ai";
        if ("ollama".equals(providerId)) return preferences == null ? "" : preferences.getOllamaBaseUrl();
        if ("custom".equals(providerId)) return preferences == null ? "" : preferences.getCustomBaseUrl();
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

    /** Short brand letter shown on provider rows in the model picker. */
    public static String brandMark(String providerId) {
        if ("experiential".equals(providerId)) return "E";
        if ("openrouter".equals(providerId)) return "O";
        if ("groq".equals(providerId)) return "G";
        if ("moonshot".equals(providerId)) return "K";
        if ("nvidia".equals(providerId)) return "N";
        if ("mistral".equals(providerId)) return "M";
        if ("anthropic".equals(providerId)) return "A";
        if ("openai".equals(providerId)) return "AI";
        if ("ollama".equals(providerId)) return "🦙";
        if ("custom".equals(providerId)) return "⚡";
        if ("google".equals(providerId)) return "G";
        if ("xai".equals(providerId)) return "X";
        if ("huggingface".equals(providerId)) return "H";
        if ("perplexity".equals(providerId)) return "P";
        return "·";
    }
}
