package com.kairo.app;

import com.kairo.app.data.Artifact;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArtifactTest {
    @Test
    public void infersCommonSourceLanguages() {
        assertEquals("java", Artifact.inferLanguage("MainActivity.java"));
        assertEquals("kotlin", Artifact.inferLanguage("MainActivity.kt"));
        assertEquals("typescript", Artifact.inferLanguage("client.ts"));
        assertEquals("shell", Artifact.inferLanguage("check.sh"));
        assertEquals("json", Artifact.inferLanguage("models.json"));
        assertEquals("markdown", Artifact.inferLanguage("README.md"));
        assertEquals("text", Artifact.inferLanguage("notes.txt"));
    }
}
