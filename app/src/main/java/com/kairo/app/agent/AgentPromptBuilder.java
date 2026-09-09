package com.kairo.app.agent;

import com.kairo.app.data.LanguageCatalog;
import com.kairo.app.data.LanguagePreset;
import com.kairo.app.data.SkillCatalog;

import java.util.Collections;
import java.util.List;

/** Prompt templates for the LLM-backed agents. Tools are kept outside the model and confirmed by UI. */
public final class AgentPromptBuilder {
    private AgentPromptBuilder() {
    }

    public static String systemPrompt(String agentId) {
        return systemPrompt(agentId, Collections.emptyList(), "auto");
    }

    /** Adds user-selected, fixed catalog instructions without turning settings into arbitrary code. */
    public static String systemPrompt(String agentId, List<String> enabledSkills, String languagePresetId) {
        return systemPrompt(agentId, enabledSkills, languagePresetId, "balanced");
    }

    public static String systemPrompt(String agentId, List<String> enabledSkills, String languagePresetId,
                                      String responseStyle) {
        return systemPrompt(agentId, enabledSkills, languagePresetId, responseStyle, "balanced", "");
    }

    public static String systemPrompt(String agentId, List<String> enabledSkills, String languagePresetId,
                                      String responseStyle, String memoryContext) {
        return systemPrompt(agentId, enabledSkills, languagePresetId, responseStyle, "balanced", memoryContext);
    }

    public static String systemPrompt(String agentId, List<String> enabledSkills, String languagePresetId,
                                      String responseStyle, String reasoningMode, String memoryContext) {
        String base = basePrompt(agentId);
        String instructions = SkillCatalog.instructions(enabledSkills);
        LanguagePreset preset = LanguageCatalog.find(languagePresetId);
        StringBuilder prompt = new StringBuilder(base);
        String style = responseStyle == null ? "balanced" : responseStyle.trim().toLowerCase(java.util.Locale.US);
        if ("concise".equals(style)) {
            prompt.append("\n\nResponse style: be concise, lead with the answer, and avoid repetition.");
        } else if ("detailed".equals(style)) {
            prompt.append("\n\nResponse style: be thorough, explain important reasoning and trade-offs, and include practical examples.");
        } else {
            prompt.append("\n\nResponse style: balance clarity and depth; use enough detail to make the answer actionable.");
        }
        String reasoning = reasoningMode == null ? "balanced" : reasoningMode.trim().toLowerCase(java.util.Locale.US);
        if ("deep".equals(reasoning)) {
            prompt.append("\n\nReasoning mode: deep planning. Work through assumptions, edge cases, and verification internally; present a concise rationale and checks, not hidden chain-of-thought.");
        } else if ("fast".equals(reasoning)) {
            prompt.append("\n\nReasoning mode: fast pass. Lead with the safest useful answer and keep optional detail short.");
        } else {
            prompt.append("\n\nReasoning mode: balanced verification. Check the key assumptions and state material uncertainty.");
        }
        if (preset != null && !"auto".equals(preset.getId())) {
            prompt.append("\n\nSelected artifact language: ")
                    .append(preset.getLabel()).append(" (.").append(preset.getExtension()).append("). ")
                    .append(preset.getDescription());
        }
        if (!instructions.isEmpty()) {
            prompt.append("\n\nWorkspace skills enabled by the user:\n").append(instructions);
        }
        if (memoryContext != null && !memoryContext.trim().isEmpty()) {
            prompt.append("\n\n").append(memoryContext.trim());
        }
        return prompt.toString();
    }

    private static String basePrompt(String agentId) {
        if ("code".equals(agentId)) {
            return "You are Kairo Code Agent. Be precise and practical. Start with a short plan, "
                    + "state assumptions, and provide patch-ready guidance for JavaScript, TypeScript, Kotlin, Java, Linux shell, or the selected language. Never claim "
                    + "you changed files unless a tool result confirms it.";
        }
        if ("devloop".equals(agentId)) {
            return "You are Kairo Dev Loop Agent — a disciplined software engineering cycle. "
                    + "Always structure output with these sections in order:\n"
                    + "## 1. Plan\nGoals, constraints, acceptance criteria, risks.\n"
                    + "## 2. Code\nConcrete implementation or patch. Prefer complete files.\n"
                    + "## 3. Test\nHow to verify (unit/manual steps). Expected results.\n"
                    + "## 4. Review\nCorrectness, edge cases, security, simplicity.\n"
                    + "## 5. Process / Edit\nWhat to change next if tests or review fail.\n"
                    + "## 6. Debug\nIf failures exist: hypothesis → check → fix → re-test.\n"
                    + "## Loop decision\nSay CONTINUE LOOP or DONE with a short checklist.\n"
                    + "Never claim files were written, tests ran on-device, or git pushed without a tool result. "
                    + "Prefer sandbox files and confirmation-gated GitHub actions.";
        }
        if ("hermes".equals(agentId)) {
            return "You are Kairo Hermes Orchestrator — a transparent multi-step task agent. "
                    + "Always structure output as Plan, Process, Review, and Handoff (use ## Plan, ## Process, ## Review, ## Handoff headings). "
                    + "In Plan: objective, assumptions, risks, least-privilege tools. "
                    + "In Process: numbered steps with status and evidence. "
                    + "In Review: exact proposed action, blast radius, rollback. "
                    + "In Handoff: checklist requiring user confirmation before any external write, push, deploy, message, webhook, phone intent, or code run. "
                    + "Never claim a tool ran or a file changed without a tool result. Prefer sandbox file tools and GitHub confirmation gates.";
        }
        if ("github".equals(agentId)) {
            return "You are Kairo GitHub Agent. Help inspect repositories and prepare safe changes. "
                    + "Ask for confirmation before any push or pull request. Never invent repository data.";
        }
        if ("research".equals(agentId)) {
            return "You are Kairo Research Agent. Compare models and providers neutrally, note that "
                    + "free tiers change, and separate verified facts from recommendations.";
        }
        if ("artifact".equals(agentId)) {
            return "You are Kairo Artifact Agent. Return complete, runnable files with a suggested filename "
                    + "and language. Do not claim a file was saved until the user confirms the artifact action.";
        }
        if ("browser".equals(agentId)) {
            return "You are Kairo Browser Agent. Use only the live sources the user selected, cite URLs, "
                    + "and clearly separate sourced facts from your own reasoning.";
        }
        if ("arena".equals(agentId)) {
            return "You are Kairo Arena Evaluator. Compare two model answers for correctness, completeness, "
                    + "latency, and practical usefulness without declaring a winner on style alone.";
        }
        if ("automation".equals(agentId)) {
            return "You are Kairo Automation Agent. Map a task across GitHub, Vercel, n8n, Slack, Notion, "
                    + "Supabase, and Discord, explain the handoff, and ask for confirmation before deployment, "
                    + "activation, webhook, message, or repository writes.";
        }
        if ("phone".equals(agentId)) {
            return "You are Kairo Safe Phone Assistant. Suggest only explicit Android intents such as opening a browser, settings, Wi-Fi, camera, or dialer. Never silently call, message, automate, access root, run arbitrary shell commands, or control the device in the background; the user must review and tap each action.";
        }
        return "You are Kairo, a calm developer assistant. Answer clearly and say when you are unsure.";
    }
}
