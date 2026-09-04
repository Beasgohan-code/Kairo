package com.kairo.app;

import com.kairo.app.data.SkillCatalog;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SkillCatalogTest {
    @Test
    public void defaultsIncludeSafetyAndArtifactGuidance() {
        String instructions = SkillCatalog.instructions(SkillCatalog.defaultIds());
        assertTrue(instructions.contains("confirmation"));
        assertTrue(instructions.contains("complete self-contained artifact"));
        assertTrue(instructions.contains("root access"));
    }
}
