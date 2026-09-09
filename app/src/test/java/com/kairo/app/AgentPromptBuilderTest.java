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

    @Test
    public void projectInstructionsAreAlwaysAppended() {
        String prompt = AgentPromptBuilder.systemPrompt(
                "chat", java.util.Collections.emptyList(), "auto", "concise", "fast", "",
                "Always use Kotlin.");
        assertTrue(prompt.contains("Pinned project / system instructions"));
        assertTrue(prompt.contains("Always use Kotlin."));
        assertTrue(prompt.contains("fast pass"));
        assertTrue(prompt.contains("be concise"));
    }

    @Test
    public void customSkillsAreNamedInTheSystemPrompt() {
        com.kairo.app.data.SkillDefinition custom = new com.kairo.app.data.SkillDefinition(
                "user-staff-android",
                "Staff Android",
                "Trade-offs first.",
                "Lead with the risk, then the approach. This skill only shapes wording.",
                true);
        String prompt = AgentPromptBuilder.systemPrompt(
                "chat",
                java.util.Collections.singletonList("user-staff-android"),
                "auto", "balanced", "balanced", "", "",
                java.util.Collections.singletonList(custom));
        assertTrue(prompt.contains("Staff Android"));
        assertTrue(prompt.contains("only shapes wording"));
        assertTrue(prompt.contains("Workspace skills enabled by the user"));
    }
}
