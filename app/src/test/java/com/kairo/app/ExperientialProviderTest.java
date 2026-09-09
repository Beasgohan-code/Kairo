package com.kairo.app;

import com.kairo.app.core.ProviderConfig;
import com.kairo.app.data.ModelCatalog;
import com.kairo.app.data.ModelInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExperientialProviderTest {
    @Test
    public void providerMetadataPointsAtExperientialGateway() {
        assertEquals("Experiential Labs", ProviderConfig.displayName("experiential"));
        assertEquals("https://api.experientiallabs.ai/v1",
                ProviderConfig.baseUrl("experiential", null));
        assertTrue(ProviderConfig.needsApiKey("experiential"));
        assertTrue(ProviderConfig.apiKeyHint("experiential").toLowerCase().contains("experiential"));
    }

    @Test
    public void catalogSurfacesFrontierAndPromoModels() {
        int count = 0;
        boolean hasAstra = false;
        for (ModelInfo model : ModelCatalog.forProvider("experiential")) {
            count++;
            if ("gpt-6-astra".equals(model.getId())) hasAstra = true;
        }
        assertTrue(count >= 10);
        assertTrue(hasAstra);
    }
}
