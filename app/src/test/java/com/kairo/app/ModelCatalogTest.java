package com.kairo.app;

import com.kairo.app.data.ModelCatalog;
import com.kairo.app.data.ModelInfo;

import org.junit.Test;

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
}
