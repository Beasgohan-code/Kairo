package com.kairo.app;

import com.kairo.app.data.ModelCatalog;
import com.kairo.app.data.ModelInfo;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NvidiaCatalogTest {
    @Test
    public void hasAVisibleCandidateCatalogWithoutPromisingFreeAccess() {
        int candidates = 0;
        for (ModelInfo model : ModelCatalog.forProvider("nvidia")) {
            if (model.isCandidate()) {
                candidates++;
                assertTrue(!model.isFreeRoute());
            }
        }
        assertTrue(candidates >= 50);
    }
}
