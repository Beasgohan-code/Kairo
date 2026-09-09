package com.kairo.app;

import com.kairo.app.agent.AgentModes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AgentModesTest {
    @Test
    public void pickerIdsAndLabelsStayAlignedAndIncludeDevLoop() {
        String[] ids = AgentModes.pickerIds();
        String[] labels = AgentModes.pickerLabels();
        assertEquals(ids.length, labels.length);
        assertEquals(10, ids.length);
        assertEquals("chat", ids[0]);
        assertEquals("devloop", ids[3]);
        assertEquals("phone", ids[ids.length - 1]);
        assertTrue(labels[3].contains("Dev Loop"));
        assertTrue(AgentModes.isKnown("hermes"));
        assertFalse(AgentModes.isKnown("unknown-mode"));
        assertEquals("Chat mode", AgentModes.labelFor(null));
        assertEquals("chat", AgentModes.normalize("nope"));
        assertEquals("code", AgentModes.normalize("code"));
    }
}
