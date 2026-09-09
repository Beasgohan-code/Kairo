package com.kairo.app;

import com.kairo.app.core.ApiKeyDetector;
import com.kairo.app.core.MemoryStore;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ApiKeyDetectorTest {
    @Test
    public void detectsKnownProviderPrefixesWithoutExposingTheFullValueInTheMask() {
        ApiKeyDetector.DetectedCredential credential = ApiKeyDetector.detect(
                "paste this into Kairo: gsk_exampleCredentialValue123456789");
        assertEquals("groq", credential.getProviderId());
        assertTrue(credential.masked().startsWith("gsk_"));
        assertTrue(!credential.masked().contains("exampleCredentialValue123456789"));
    }

    @Test
    public void doesNotOfferMemoryForCredentialText() {
        assertEquals("", MemoryStore.candidateFromText(
                "remember that my key is nvapi_exampleCredentialValue123456789"));
    }

    @Test
    public void ignoresOrdinaryConversation() {
        assertNull(ApiKeyDetector.detect("Please explain how streaming works."));
        assertEquals("I prefer short answers", MemoryStore.candidateFromText(
                "I prefer short answers."));
    }

    @Test
    public void detectsNewProvidersAndGenericOpenAiLast() {
        assertEquals("xai", ApiKeyDetector.detect("xai-abcdefghijklmnopqrstuvwxyz123456").getProviderId());
        assertEquals("huggingface", ApiKeyDetector.detect("hf_abcdefghijklmnopqrstuvwxyz123456").getProviderId());
        assertEquals("perplexity", ApiKeyDetector.detect("pplx-abcdefghijklmnopqrstuvwxyz123456").getProviderId());
        assertEquals("google", ApiKeyDetector.detect("AIzaSyDummyGoogleAiStudioKey1234567").getProviderId());
        String genericOpenAi = "sk-" + "abcdefghijklmnopqrstuvwxyz1234567890ABCD";
        ApiKeyDetector.DetectedCredential openai = ApiKeyDetector.detect(genericOpenAi);
        assertEquals("openai", openai.getProviderId());
        assertTrue(ApiKeyDetector.redact("secret " + genericOpenAi)
                .contains("[OpenAI key redacted]"));
        assertNull(ApiKeyDetector.detect("deadbeefcafebabe0123456789abcdef"));
    }
}
