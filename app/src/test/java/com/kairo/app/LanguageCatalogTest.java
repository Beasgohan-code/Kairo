package com.kairo.app;

import com.kairo.app.data.Artifact;
import com.kairo.app.data.LanguageCatalog;
import com.kairo.app.data.LanguagePreset;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LanguageCatalogTest {
    @Test
    public void includesTheRequestedProfessionalCodePresets() {
        boolean javascript = false, typescript = false, kotlin = false, java = false, shell = false;
        boolean cpp = false, c = false, assembly = false, css = false;
        boolean python = false, xml = false, md = false, txt = false, zip = false;
        for (LanguagePreset preset : LanguageCatalog.all()) {
            javascript |= "javascript".equals(preset.getId());
            typescript |= "typescript".equals(preset.getId());
            kotlin |= "kotlin".equals(preset.getId());
            java |= "java".equals(preset.getId());
            shell |= "shell".equals(preset.getId());
            cpp |= "cpp".equals(preset.getId());
            c |= "c".equals(preset.getId());
            assembly |= "assembly".equals(preset.getId());
            css |= "css".equals(preset.getId());
            python |= "python".equals(preset.getId());
            xml |= "xml".equals(preset.getId());
            md |= "markdown".equals(preset.getId());
            txt |= "text".equals(preset.getId());
            zip |= "zip".equals(preset.getId());
        }
        assertTrue(javascript && typescript && kotlin && java && shell);
        assertTrue(cpp && c && assembly && css);
        assertTrue(python && xml && md && txt && zip);
        assertEquals("kt", LanguageCatalog.find("kotlin").getExtension());
        assertEquals("cpp", LanguageCatalog.find("cpp").getExtension());
        assertEquals("s", LanguageCatalog.find("assembly").getExtension());
        assertEquals("py", LanguageCatalog.find("python").getExtension());
        assertNotNull(LanguageCatalog.find("unknown"));
        assertFalse(LanguageCatalog.starterTemplate("python").isEmpty());
        assertTrue(LanguageCatalog.starterTemplate("java").contains("Hello from Kairo"));
    }

    @Test
    public void artifactInfersNativeAndWebLanguages() {
        assertEquals("cpp", Artifact.inferLanguage("hotpath.cpp"));
        assertEquals("c", Artifact.inferLanguage("core.c"));
        assertEquals("assembly", Artifact.inferLanguage("entry.s"));
        assertEquals("css", Artifact.inferLanguage("theme.css"));
        assertEquals("typescript", Artifact.inferLanguage("util.ts"));
        assertEquals("python", Artifact.inferLanguage("app.py"));
        assertEquals("xml", Artifact.inferLanguage("layout.xml"));
        assertEquals("markdown", Artifact.inferLanguage("README.md"));
        assertEquals("text", Artifact.inferLanguage("notes.txt"));
        assertEquals("zip", Artifact.inferLanguage("bundle.zip"));
        assertEquals("yaml", Artifact.inferLanguage("ci.yml"));
        assertEquals("go", Artifact.inferLanguage("main.go"));
    }
}
