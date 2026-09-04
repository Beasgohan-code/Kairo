package com.kairo.app;

import com.kairo.app.agent.AgentPromptBuilder;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AgentPromptBuilderTest {
    @Test
    public void hermesPromptKeepsTheRunTransparent() {
        String prompt = AgentPromptBuilder.systemPrompt(
                "hermes", Arrays.asList("safe-actions", "artifact"), "typescript", "detailed", "deep",
                "User-approved memory (use only when relevant):\n- [preference] I prefer concise diffs");
        assertTrue(prompt.contains("Plan, Process, Review, and Handoff"));
        assertTrue(prompt.contains("TypeScript"));
        assertTrue(prompt.contains("confirmation"));
        assertTrue(prompt.contains("complete self-contained artifact"));
        assertTrue(prompt.contains("thorough"));
        assertTrue(prompt.contains("deep planning"));
        assertTrue(prompt.contains("I prefer concise diffs"));
    }
}
