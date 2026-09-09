package com.kairo.app;

import com.kairo.app.core.SlashCommands;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SlashCommandsTest {
    @Test
    public void helpAndModeCommandsAreConsumedOnDevice() {
        SlashCommands.Result help = SlashCommands.parse("/help");
        assertTrue(help.isMatched());
        assertTrue(help.isConsumeOnly());
        assertEquals("help", help.getHelpTopic());

        SlashCommands.Result deep = SlashCommands.parse("/deep");
        assertTrue(deep.isConsumeOnly());
        assertEquals("deep", deep.getReasoningMode());

        SlashCommands.Result think = SlashCommands.parse("/think write a plan");
        assertTrue(think.isMatched());
        assertFalse(think.isConsumeOnly());
        assertEquals("deep", think.getReasoningMode());
        assertEquals("write a plan", think.getPrompt());
    }

    @Test
    public void codeAndFileCommandsSetAgentsWithoutTouchingUnknownText() {
        SlashCommands.Result code = SlashCommands.parse("/code fix the parser");
        assertEquals("code", code.getAgentId());
        assertEquals("fix the parser", code.getPrompt());

        SlashCommands.Result file = SlashCommands.parse("/file");
        assertEquals("artifact", file.getAgentId());
        assertTrue(file.getPrompt().contains("fenced code block"));

        assertFalse(SlashCommands.parse("hello").isMatched());
        assertFalse(SlashCommands.parse("/unknown thing").isMatched());
        assertTrue(SlashCommands.helpText().contains("/summarize"));
        assertTrue(SlashCommands.isKnownCommand("/arena"));
    }

    @Test
    public void skillCreatorIsConsumedOnDeviceWithTheBriefPreserved() {
        SlashCommands.Result empty = SlashCommands.parse("/skill-creator");
        assertTrue(empty.isMatched());
        assertTrue(empty.isConsumeOnly());
        assertEquals("skill-creator", empty.getHelpTopic());
        assertEquals("", empty.getPrompt());

        SlashCommands.Result brief = SlashCommands.parse(
                "/skill-creator Answer like a staff Android engineer: trade-offs first.");
        assertEquals("skill-creator", brief.getHelpTopic());
        assertEquals("Answer like a staff Android engineer: trade-offs first.", brief.getPrompt());
        assertTrue(SlashCommands.parse("/skill review diffs like a staff reviewer").isMatched());
        assertEquals("skills", SlashCommands.parse("/skills").getHelpTopic());
        assertTrue(SlashCommands.helpText().contains("/skill-creator"));
    }
}
