package com.kairo.app;

import com.kairo.app.data.ModelCatalog;
import com.kairo.app.data.ModelInfo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ModelCatalogTest {
    @Test
    public void starterCatalogContainsFreeAndLocalOptions() {
        boolean hasFree = false;
        boolean hasLocal = false;
        for (ModelInfo model : ModelCatalog.all()) {
            hasFree |= model.isFreeRoute();
            hasLocal |= model.isLocal();
        }
        assertTrue(hasFree);
        assertTrue(hasLocal);
        assertNotNull(ModelCatalog.find("openrouter", "deepseek/deepseek-r1:free"));
        assertNotNull(ModelCatalog.find("moonshot", "kimi-k3"));
        assertNotNull(ModelCatalog.find("moonshot", "kimi-k2.7-code"));
    }

    @Test
    public void experientialLabsIncludesGpt6AstraAndPromoRoutes() {
        ModelInfo astra = ModelCatalog.find("experiential", "gpt-6-astra");
        assertNotNull(astra);
        assertFalse(astra.isCandidate());
        assertNotNull(ModelCatalog.find("experiential", "claude-fable-5.1"));
        assertNotNull(ModelCatalog.find("experiential", "gpt-5.6-luna"));
        assertNotNull(ModelCatalog.find("experiential", "deepseek-v4-flash"));
        assertNotNull(ModelCatalog.find("experiential", "qwen3.8-27b"));
        boolean hasExperientialFree = false;
        for (ModelInfo model : ModelCatalog.forProvider("experiential")) {
            hasExperientialFree |= model.isFreeRoute();
        }
        assertTrue(hasExperientialFree);
    }
}
