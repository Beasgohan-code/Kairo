package com.kairo.app;

import com.kairo.app.data.LanguageCatalog;
import com.kairo.app.data.LanguagePreset;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LanguageCatalogTest {
    @Test
    public void includesTheRequestedProfessionalCodePresets() {
        boolean javascript = false;
        boolean typescript = false;
        boolean kotlin = false;
        boolean java = false;
        boolean shell = false;
        for (LanguagePreset preset : LanguageCatalog.all()) {
            javascript |= "javascript".equals(preset.getId());
            typescript |= "typescript".equals(preset.getId());
            kotlin |= "kotlin".equals(preset.getId());
            java |= "java".equals(preset.getId());
            shell |= "shell".equals(preset.getId());
        }
        assertTrue(javascript && typescript && kotlin && java && shell);
        assertEquals("kt", LanguageCatalog.find("kotlin").getExtension());
        assertNotNull(LanguageCatalog.find("unknown"));
    }
}
