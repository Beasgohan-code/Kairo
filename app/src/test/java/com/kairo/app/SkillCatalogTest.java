package com.kairo.app;

import com.kairo.app.data.SkillCatalog;
import com.kairo.app.data.SkillDefinition;

import java.util.Collections;

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

    @Test
    public void extraCustomSkillsAreNamedWhenEnabled() {
        SkillDefinition extra = new SkillDefinition(
                "user-editor", "Editorial voice", "Cut filler.",
                "Prefer specific verbs. This skill only shapes wording.", true);
        String text = SkillCatalog.instructions(
                Collections.singletonList("user-editor"),
                Collections.singletonList(extra));
        assertTrue(text.contains("Editorial voice"));
        assertTrue(text.contains("only shapes wording"));
    }
}
