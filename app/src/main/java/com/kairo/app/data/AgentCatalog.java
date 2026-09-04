package com.kairo.app.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AgentCatalog {
    private AgentCatalog() {
    }

    public static List<AgentDefinition> all() {
        return Collections.unmodifiableList(Arrays.asList(
                new AgentDefinition(
                        "code",
                        "Code Agent",
                        "BUILD WITH CONFIDENCE",
                        "Turn a rough idea into a scoped plan, implementation checklist, and review pass.",
                        "Plan · Explain · Review",
                        true),
                new AgentDefinition(
                        "hermes",
                        "Hermes Orchestrator",
                        "PLAN · EXECUTE · REVIEW · HAND OFF",
                        "Coordinate a transparent multi-step run across chat, artifacts, research, sandbox diagnostics, and confirmed connectors.",
                        "Plan · Process · Review · Handoff",
                        true),
                new AgentDefinition(
                        "github",
                        "GitHub Agent",
                        "SHIP WITH GUARDRAILS",
                        "Pull repository context and issues, prepare changes, and push only after you confirm.",
                        "Pull · Inspect · Push · PR",
                        true),
                new AgentDefinition(
                        "cli",
                        "CLI Agent",
                        "LOCAL TOOLBOX",
                        "Run a small allow-list of safe diagnostics inside the Android environment.",
                        "pwd · ls · git status · git diff",
                        false),
                new AgentDefinition(
                        "phone",
                        "Safe Phone Assistant",
                        "VISIBLE DEVICE HELP",
                        "Open a browser, settings, Wi-Fi, camera, or dialer only after a clear review and confirmation.",
                        "Review · Confirm · Open",
                        false),
                new AgentDefinition(
                        "research",
                        "Research Agent",
                        "COMPARE THE OPTIONS",
                        "Keep a provider-neutral view of models, limits, and the trade-offs behind each choice.",
                        "Compare · Summarize · Decide",
                        true),
                new AgentDefinition(
                        "artifact",
                        "Artifact Agent",
                        "TURN IDEAS INTO FILES",
                        "Take a generated code block or a blank canvas and turn it into a reviewable private file, then run or syntax-check a trusted draft.",
                        "Create · Preview · Edit · Run / check · Share",
                        false),
                new AgentDefinition(
                        "browser",
                        "Browser Agent",
                        "GROUND ANSWERS IN SOURCES",
                        "Search live web results, show the source list, and let you decide what context enters Chat.",
                        "Search · Cite · Select",
                        true),
                new AgentDefinition(
                        "automation",
                        "Automation Agent",
                        "CONNECT THE WORKFLOW",
                        "Coordinate GitHub, Vercel, n8n, Slack, Notion, Supabase, and Discord handoffs while keeping every external write explicit.",
                        "Inspect · Deploy · Trigger · Review",
                        true),
                new AgentDefinition(
                        "arena",
                        "Arena Evaluator",
                        "COMPARE BEFORE YOU COMMIT",
                        "Run one prompt against two models in parallel and inspect the trade-offs side by side.",
                        "Parallel · Stream · Compare",
                        true)
        ));
    }
}
