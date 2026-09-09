package com.kairo.app;

import com.kairo.app.core.ProviderConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProviderConfigTest {
    @Test
    public void includesXaiGeminiHuggingFaceAndPerplexity() {
        assertEquals("xAI", ProviderConfig.displayName("xai"));
        assertEquals("Google Gemini", ProviderConfig.displayName("google"));
        assertEquals("Hugging Face", ProviderConfig.displayName("huggingface"));
        assertEquals("Perplexity", ProviderConfig.displayName("perplexity"));
        assertEquals("https://api.x.ai/v1", ProviderConfig.baseUrl("xai", null));
        assertEquals("https://generativelanguage.googleapis.com/v1beta/openai",
                ProviderConfig.baseUrl("google", null));
        assertEquals("https://router.huggingface.co/v1", ProviderConfig.baseUrl("huggingface", null));
        assertEquals("https://api.perplexity.ai", ProviderConfig.baseUrl("perplexity", null));
        assertTrue(ProviderConfig.needsApiKey("xai"));
        assertEquals("X", ProviderConfig.brandMark("xai"));
    }
}
