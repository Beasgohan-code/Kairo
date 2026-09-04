package com.kairo.app.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, useful starter catalog. Provider / model inventories change often, so the
 * Models screen also supports refreshing a provider's live /models endpoint.
 */
public final class ModelCatalog {
    private static final Object LOCK = new Object();
    private static final Map<String, ModelInfo> DISCOVERED = new LinkedHashMap<>();

    private static final List<ModelInfo> CURATED = Collections.unmodifiableList(Arrays.asList(
            // OpenRouter routes. A :free suffix means the route is currently advertised as free;
            // availability and limits remain controlled by the provider.
            model("deepseek/deepseek-r1:free", "DeepSeek R1", "openrouter",
                    "Reasoning model on a community free route", true, false, "128K",
                    "Free route; limits vary"),
            model("deepseek/deepseek-chat-v3-0324:free", "DeepSeek V3", "openrouter",
                    "Fast general chat on a community free route", true, false, "64K",
                    "Free route; limits vary"),
            model("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B", "openrouter",
                    "Strong open model for everyday work", true, false, "128K",
                    "Free route; limits vary"),
            model("google/gemini-2.0-flash-exp:free", "Gemini Flash", "openrouter",
                    "Quick multimodal-friendly general model", true, false, "1M",
                    "Free route; limits vary"),
            model("qwen/qwen3-235b-a22b:free", "Qwen 3 235B", "openrouter",
                    "Large open model with strong reasoning", true, false, "128K",
                    "Free route; limits vary"),
            model("mistralai/mistral-small-3.1-24b-instruct:free", "Mistral Small 3.1", "openrouter",
                    "Compact, capable open instruction model", true, false, "128K",
                    "Free route; limits vary"),

            // Groq's fast inference catalog. These entries may have a free developer tier.
            model("llama-3.3-70b-versatile", "Llama 3.3 70B", "groq",
                    "Very fast general-purpose inference", true, false, "128K",
                    "Free developer tier may apply"),
            model("llama-3.1-8b-instant", "Llama 3.1 8B Instant", "groq",
                    "Low-latency chat and utility tasks", true, false, "128K",
                    "Free developer tier may apply"),
            model("gemma2-9b-it", "Gemma 2 9B", "groq",
                    "Small open model for quick tasks", true, false, "8K",
                    "Free developer tier may apply"),
            model("qwen/qwen3-32b", "Qwen 3 32B", "groq",
                    "Reasoning-capable open model", true, false, "128K",
                    "Free developer tier may apply"),

            // Kimi / Moonshot models are candidates because model IDs and regional availability change.
            candidateProvider("kimi-k3", "Kimi K3", "moonshot",
                    "Kimi long-context multimodal and deep-reasoning candidate", "1M"),
            candidateProvider("kimi-k2.7-code", "Kimi K2.7 Code", "moonshot",
                    "Kimi coding agent candidate with thinking mode", "256K"),
            candidateProvider("kimi-k2.7-code-highspeed", "Kimi K2.7 Code Highspeed", "moonshot",
                    "Higher-speed Kimi coding candidate", "256K"),
            candidateProvider("kimi-k2.6", "Kimi K2.6", "moonshot",
                    "Kimi general reasoning and agent candidate", "256K"),
            candidateProvider("kimi-k2-thinking", "Kimi K2 Thinking", "moonshot",
                    "Kimi deep-reasoning candidate; verify account access with live refresh", "256K"),
            candidateProvider("moonshot-v1-128k", "Moonshot V1 128K", "moonshot",
                    "Long-context Kimi / Moonshot candidate", "128K"),

            // NVIDIA's catalog changes frequently. These are candidate IDs only: the live refresh
            // action is authoritative for this account, region, quota, and currently exposed route.
            candidate("meta/llama-3.1-8b-instruct", "Llama 3.1 8B", "NVIDIA hosted open model", "128K"),
            candidate("meta/llama-3.1-70b-instruct", "Llama 3.1 70B", "Large Llama instruction model", "128K"),
            candidate("meta/llama-3.1-405b-instruct", "Llama 3.1 405B", "Largest Llama 3.1 instruction model", "128K"),
            candidate("meta/llama-3.2-1b-instruct", "Llama 3.2 1B", "Compact Llama instruction model", "128K"),
            candidate("meta/llama-3.2-3b-instruct", "Llama 3.2 3B", "Small Llama instruction model", "128K"),
            candidate("meta/llama-3.2-11b-vision-instruct", "Llama 3.2 11B Vision", "Vision-capable Llama candidate", "128K"),
            candidate("meta/llama-3.2-90b-vision-instruct", "Llama 3.2 90B Vision", "Large vision-capable Llama candidate", "128K"),
            candidate("meta/llama-3.3-70b-instruct", "Llama 3.3 70B", "Large Llama instruction model", "128K"),
            candidate("meta/llama-4-scout-17b-16e-instruct", "Llama 4 Scout", "Mixture-of-experts multimodal candidate", "512K"),
            candidate("meta/llama-4-maverick-17b-128e-instruct", "Llama 4 Maverick", "Mixture-of-experts multimodal candidate", "512K"),
            candidate("qwen/qwen2.5-7b-instruct", "Qwen 2.5 7B", "Open instruction model", "128K"),
            candidate("qwen/qwen2.5-14b-instruct", "Qwen 2.5 14B", "Open instruction model", "128K"),
            candidate("qwen/qwen2.5-32b-instruct", "Qwen 2.5 32B", "Open instruction model", "128K"),
            candidate("qwen/qwen2.5-72b-instruct", "Qwen 2.5 72B", "Open instruction model", "128K"),
            candidate("qwen/qwen2.5-coder-7b-instruct", "Qwen 2.5 Coder 7B", "Code-focused open model", "128K"),
            candidate("qwen/qwen2.5-coder-14b-instruct", "Qwen 2.5 Coder 14B", "Code-focused open model", "128K"),
            candidate("qwen/qwen2.5-coder-32b-instruct", "Qwen 2.5 Coder 32B", "Code-focused open model", "128K"),
            candidate("qwen/qwq-32b", "QwQ 32B", "Reasoning-focused open model", "128K"),
            candidate("qwen/qwen3-32b", "Qwen 3 32B", "Reasoning-capable open model", "128K"),
            candidate("qwen/qwen3-235b-a22b", "Qwen 3 235B", "Large mixture-of-experts candidate", "128K"),
            candidate("deepseek-ai/deepseek-r1", "DeepSeek R1", "Reasoning model candidate", "128K"),
            candidate("deepseek-ai/deepseek-v3", "DeepSeek V3", "General open model candidate", "128K"),
            candidate("deepseek-ai/deepseek-r1-distill-qwen-32b", "DeepSeek R1 Distill Qwen 32B", "Distilled reasoning candidate", "128K"),
            candidate("deepseek-ai/deepseek-r1-distill-llama-70b", "DeepSeek R1 Distill Llama 70B", "Distilled reasoning candidate", "128K"),
            candidate("mistralai/mistral-small-24b-instruct-2501", "Mistral Small 24B", "Efficient instruction model", "32K"),
            candidate("mistralai/mistral-large-2-instruct", "Mistral Large 2", "Large multilingual instruction model", "128K"),
            candidate("mistralai/mixtral-8x7b-instruct-v0.1", "Mixtral 8x7B", "Sparse mixture-of-experts model", "32K"),
            candidate("mistralai/mixtral-8x22b-instruct-v0.1", "Mixtral 8x22B", "Large sparse mixture-of-experts model", "64K"),
            candidate("mistralai/mistral-nemo-12b-instruct", "Mistral NeMo 12B", "Compact multilingual instruction model", "128K"),
            candidate("mistralai/mistral-nemotron", "Mistral Nemotron", "NVIDIA-tuned Mistral candidate", "128K"),
            candidate("google/gemma-2-2b-it", "Gemma 2 2B", "Compact open instruction model", "8K"),
            candidate("google/gemma-2-9b-it", "Gemma 2 9B", "Open instruction model", "8K"),
            candidate("google/gemma-2-27b-it", "Gemma 2 27B", "Large open instruction model", "8K"),
            candidate("google/gemma-3-1b-it", "Gemma 3 1B", "Compact multimodal-friendly candidate", "32K"),
            candidate("google/gemma-3-4b-it", "Gemma 3 4B", "Compact multimodal-friendly candidate", "128K"),
            candidate("google/gemma-3-12b-it", "Gemma 3 12B", "Multimodal-friendly instruction candidate", "128K"),
            candidate("google/gemma-3-27b-it", "Gemma 3 27B", "Large multimodal-friendly candidate", "128K"),
            candidate("nvidia/llama-3.1-nemotron-ultra-253b-v1", "Nemotron Ultra 253B", "NVIDIA reasoning candidate", "128K"),
            candidate("nvidia/llama-3.1-nemotron-nano-vl-8b-v1", "Nemotron Nano VL 8B", "NVIDIA vision-language candidate", "128K"),
            candidate("nvidia/llama-3.1-nemotron-nano-4b-v1.5", "Nemotron Nano 4B", "NVIDIA compact reasoning candidate", "128K"),
            candidate("nvidia/llama-3.1-nemotron-70b-instruct", "Nemotron 70B", "NVIDIA-tuned instruction candidate", "128K"),
            candidate("nvidia/llama-3.1-nemotron-safety-guard-8b-v3", "Nemotron Safety Guard 8B", "NVIDIA safety classifier candidate", "128K"),
            candidate("nvidia/nemotron-4-340b-instruct", "Nemotron 4 340B", "NVIDIA large instruction candidate", "4K"),
            candidate("nvidia/nemotron-mini-4b-instruct", "Nemotron Mini 4B", "NVIDIA compact instruction candidate", "32K"),
            candidate("microsoft/phi-3.5-mini-instruct", "Phi 3.5 Mini", "Compact instruction model", "128K"),
            candidate("microsoft/phi-4", "Phi 4", "Compact reasoning and coding candidate", "16K"),
            candidate("ibm/granite-3.2-8b-instruct", "Granite 3.2 8B", "Enterprise-focused open model", "128K"),
            candidate("ibm/granite-3.3-8b-instruct", "Granite 3.3 8B", "Enterprise-focused open model", "128K"),
            candidate("cohere/command-r-plus", "Command R Plus", "Retrieval and tool-use candidate", "128K"),
            candidate("ai21labs/jamba-1.5-mini-instruct", "Jamba 1.5 Mini", "Long-context hybrid candidate", "256K"),
            candidate("databricks/dbrx-instruct", "DBRX Instruct", "Open mixture-of-experts candidate", "32K"),
            candidate("writer/palmyra-med-70b", "Palmyra Med 70B", "Domain model candidate", "32K"),
            candidate("01-ai/yi-large", "Yi Large", "Large multilingual candidate", "32K"),
            candidate("upstage/solar-10.7b-instruct", "Solar 10.7B", "Compact instruction candidate", "32K"),
            candidate("snowflake/arctic", "Arctic", "Open mixture-of-experts candidate", "4K"),
            candidate("baichuan-inc/baichuan2-13b-chat", "Baichuan 2 13B", "Multilingual chat candidate", "4K"),
            candidate("thudm/chatglm3-6b", "ChatGLM3 6B", "Compact multilingual candidate", "8K"),
            candidate("bigcode/starcoder2-15b", "StarCoder2 15B", "Code generation candidate", "16K"),

            // Mistral's official OpenAI-compatible endpoint.
            model("mistral-small-latest", "Mistral Small", "mistral",
                    "Fast multilingual model for everyday work", false, false, "32K",
                    "Provider billing / limits apply"),
            model("mistral-large-latest", "Mistral Large", "mistral",
                    "Large multilingual reasoning and coding model", false, false, "128K",
                    "Provider billing / limits apply"),
            model("codestral-latest", "Codestral", "mistral",
                    "Code-focused model with long context", false, false, "256K",
                    "Provider billing / limits apply"),

            // Paid / official providers are here so users can use one interface for all keys.
            model("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "anthropic",
                    "Fast, concise assistant", false, false, "200K", "Provider billing applies"),
            model("claude-3-7-sonnet-20250219", "Claude 3.7 Sonnet", "anthropic",
                    "Deep reasoning and coding", false, false, "200K", "Provider billing applies"),
            model("gpt-4o-mini", "GPT-4o mini", "openai",
                    "Affordable general-purpose model", false, false, "128K", "Provider billing applies"),
            model("gpt-4.1-mini", "GPT-4.1 mini", "openai",
                    "Coding and everyday assistant", false, false, "1M", "Provider billing applies"),

            // Ollama stays local and does not need an API key. The user can change the LAN URL.
            model("llama3.2", "Llama 3.2 (local)", "ollama",
                    "Run locally with Ollama", true, true, "128K", "Local; no provider key"),
            model("qwen2.5:7b", "Qwen 2.5 7B (local)", "ollama",
                    "Private local chat model", true, true, "32K", "Local; no provider key"),
            model("deepseek-r1:7b", "DeepSeek R1 7B (local)", "ollama",
                    "Local reasoning model", true, true, "128K", "Local; no provider key")
    ));

    private ModelCatalog() {
    }

    private static ModelInfo model(
            String id,
            String name,
            String provider,
            String description,
            boolean free,
            boolean local,
            String context,
            String billing) {
        return new ModelInfo(id, name, provider, description, free, local, context, billing);
    }

    private static ModelInfo candidate(String id, String name, String description, String context) {
        return new ModelInfo(id, name, "nvidia", description, false, false, context,
                "Candidate ID · availability, quotas, credits, and endpoint support vary; refresh NVIDIA live catalog.", true);
    }

    private static ModelInfo candidateProvider(String id, String name, String provider,
                                               String description, String context) {
        return new ModelInfo(id, name, provider, description, false, false, context,
                "Candidate ID · availability, quotas, credits, and endpoint support vary; refresh the provider catalog.", true);
    }

    public static List<ModelInfo> all() {
        synchronized (LOCK) {
            List<ModelInfo> result = new ArrayList<>(CURATED);
            for (ModelInfo model : DISCOVERED.values()) {
                for (int index = result.size() - 1; index >= 0; index--) {
                    ModelInfo existing = result.get(index);
                    if (existing.getProviderId().equals(model.getProviderId())
                            && existing.getId().equals(model.getId())) {
                        result.remove(index);
                    }
                }
                result.add(model);
            }
            return result;
        }
    }

    public static List<ModelInfo> forProvider(String providerId) {
        List<ModelInfo> result = new ArrayList<>();
        for (ModelInfo model : all()) {
            if (model.getProviderId().equals(providerId)) {
                result.add(model);
            }
        }
        return result;
    }

    public static ModelInfo find(String providerId, String modelId) {
        for (ModelInfo model : all()) {
            if (model.getProviderId().equals(providerId) && model.getId().equals(modelId)) {
                return model;
            }
        }
        return null;
    }

    /** Merge a live provider response without losing the hand-written starter catalog. */
    public static void mergeDiscovered(List<ModelInfo> models) {
        if (models == null) {
            return;
        }
        synchronized (LOCK) {
            for (ModelInfo model : models) {
                String key = model.getProviderId() + "::" + model.getId();
                DISCOVERED.put(key, model);
            }
        }
    }

    /** Replace one provider's live snapshot so a refresh does not leave stale model IDs behind. */
    public static void replaceDiscovered(String providerId, List<ModelInfo> models) {
        if (providerId == null) return;
        synchronized (LOCK) {
            String prefix = providerId + "::";
            List<String> stale = new ArrayList<>();
            for (String key : DISCOVERED.keySet()) if (key.startsWith(prefix)) stale.add(key);
            for (String key : stale) DISCOVERED.remove(key);
            if (models == null) return;
            for (ModelInfo model : models) {
                if (model == null) continue;
                DISCOVERED.put(model.getProviderId() + "::" + model.getId(), model);
            }
        }
    }

}
