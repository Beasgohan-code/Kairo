package com.kairo.app;

import com.kairo.app.agent.ToolRegistry;
import com.kairo.app.core.Formatters;
import com.kairo.app.data.FeatureRoadmap;
import com.kairo.app.data.WhatsNew;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WhatsNewAndRoadmapTest {
    @Test
    public void releaseNotesAndIdeasArePresent() {
        assertEquals("0.14.0", WhatsNew.currentVersion());
        assertTrue(WhatsNew.releaseNotes().contains("Slash commands"));
        assertTrue(FeatureRoadmap.asText().contains("Streaming tool-calling"));
        assertTrue(FeatureRoadmap.all().size() >= 10);
        assertEquals("1.5k", Formatters.compactCount(1500));
    }

    @Test
    public void toolRegistryIncludes014Specs() {
        boolean slash = false, find = false, speak = false, pin = false, duplicate = false;
        for (com.kairo.app.agent.ToolSpec tool : ToolRegistry.all()) {
            slash |= "slash_commands".equals(tool.getName());
            find |= "find_in_conversation".equals(tool.getName());
            speak |= "speak_answer".equals(tool.getName());
            pin |= "pin_lock".equals(tool.getName());
            duplicate |= "duplicate_conversation".equals(tool.getName());
        }
        assertTrue(slash && find && speak && pin && duplicate);
        assertTrue(ToolRegistry.writeCount() > 0);
    }
}
