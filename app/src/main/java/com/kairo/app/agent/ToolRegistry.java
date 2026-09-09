package com.kairo.app.agent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Explicit, auditable tool registry for Kairo's agent harness. */
public final class ToolRegistry {
    private static final List<ToolSpec> TOOLS = Collections.unmodifiableList(Arrays.asList(
            new ToolSpec("github_repo_summary", "Repository summary",
                    "Read public metadata for an authenticated owner/name repository.", false, true),
            new ToolSpec("github_open_issues", "Open issues",
                    "Read up to twenty open issues or pull requests.", false, true),
            new ToolSpec("github_read_file", "Read a file",
                    "Read one text file from a repository and branch.", false, true),
            new ToolSpec("github_push_file", "Push one file",
                    "Create or update one text file after explicit user confirmation.", true, true),
            new ToolSpec("github_create_pr", "Create pull request",
                    "Open a pull request without merging it, after explicit confirmation.", true, true),
            new ToolSpec("safe_cli", "Safe CLI diagnostics",
                    "Run only commands accepted by CliCommandPolicy in the app sandbox.", false, false),
            new ToolSpec("sandbox_runtime_status", "Sandbox runtime status",
                    "Inspect which runtimes are already visible to the private Android process without installing tools.", false, false),
            new ToolSpec("memory_context", "Approved memory context",
                    "Read only user-approved, encrypted local memories and include bounded relevant context in a provider request.", false, false),
            new ToolSpec("hermes_workflow", "Hermes workflow",
                    "Plan, process, review, and hand off a task while keeping external writes behind user confirmation.", false, false),
            new ToolSpec("code_run", "Run or check artifact",
                    "Run trusted JavaScript, TypeScript, Python, or Java/Kotlin code only after explicit user confirmation; shell artifacts receive syntax checks only.", true, false),
            new ToolSpec("safe_phone_intent", "Safe phone intent",
                    "Open a visible browser, settings, Wi-Fi, camera, or dialer screen after explicit confirmation; never perform the action silently.", true, false),
            new ToolSpec("model_catalog", "Model catalog",
                    "Search curated and provider-discovered model options.", false, true),
            new ToolSpec("attach_text", "Text attachment",
                    "Read a user-selected text file into the current prompt.", false, false),
            new ToolSpec("web_search", "Web search",
                    "Search live sources and let the user select which snippets become context.", false, true),
            new ToolSpec("artifact_create", "Create artifact",
                    "Create a bounded private text file with a safe filename.", true, false),
            new ToolSpec("artifact_preview", "Preview artifact",
                    "Open, edit, copy, share, or delete a private generated file.", false, false),
            new ToolSpec("model_arena", "Model arena",
                    "Run the same prompt against two selected models in parallel.", false, true),
            new ToolSpec("vercel_projects", "Vercel projects",
                    "Read connected projects and deployment status from Vercel.", false, true),
            new ToolSpec("vercel_deploy", "Vercel deployment",
                    "Create a Git-backed deployment only after explicit confirmation.", true, true),
            new ToolSpec("n8n_workflows", "n8n workflows",
                    "Read workflow state and recent execution history from a configured n8n instance.", false, true),
            new ToolSpec("n8n_webhook", "n8n webhook",
                    "Send a reviewed JSON payload to one user-configured n8n webhook.", true, true),
            new ToolSpec("n8n_activate", "Activate n8n workflow",
                    "Activate one workflow only after the user confirms the workflow id.", true, true),
            new ToolSpec("slack_channels", "Slack channels",
                    "Read the connected workspace's available channels.", false, true),
            new ToolSpec("slack_message", "Slack message",
                    "Post one reviewed message to a selected channel id.", true, true),
            new ToolSpec("notion_search", "Notion search",
                    "Search pages shared with the configured Notion integration.", false, true),
            new ToolSpec("linear_issue_search", "Linear issue search",
                    "Read and search Linear issues for planning context without changing issue state.", false, true),
            new ToolSpec("supabase_preview", "Supabase table preview",
                    "Read at most twenty rows from one explicitly configured REST table.", false, true),
            new ToolSpec("discord_webhook", "Discord webhook",
                    "Send one reviewed status message through an encrypted webhook URL.", true, true),
            // Advanced / professional tools (v0.2+)
            new ToolSpec("github_list_prs", "List pull requests",
                    "Read open or recent pull requests for a repository with status and review summary.", false, true),
            new ToolSpec("github_workflow_runs", "GitHub Actions runs",
                    "Inspect recent workflow runs and their conclusions for a repository.", false, true),
            new ToolSpec("memory_search", "Memory search",
                    "Semantically search the encrypted local memory vault and surface only user-approved items.", false, false),
            new ToolSpec("artifact_diff", "Artifact diff",
                    "Compare two private artifacts or an artifact against a generated code block and show a unified diff.", false, false),
            new ToolSpec("provider_health", "Provider health check",
                    "Probe selected provider endpoints for latency and basic availability without sending user content.", false, true),
            new ToolSpec("export_conversation", "Export conversation",
                    "Export a redacted conversation transcript as Markdown or JSON after explicit confirmation.", true, false),
            // v0.5 advanced tools
            new ToolSpec("github_pr_review", "Pull request review",
                    "Fetch a PR diff and produce a structured review (summary, risks, suggested changes) without posting unless confirmed.", false, true),
            new ToolSpec("github_pr_comment", "PR comment",
                    "Post one reviewed comment on a pull request after explicit confirmation.", true, true),
            new ToolSpec("artifact_search", "Search artifacts",
                    "Keyword search across private local artifacts and inject selected snippets as context.", false, false),
            new ToolSpec("system_instructions", "Project instructions",
                    "Read or update the pinned project/system instructions that are prepended to future prompts.", true, false),
            new ToolSpec("memory_suggest", "Memory suggestion",
                    "Propose a concise memory item from the current conversation for user approval before saving.", false, false),
            new ToolSpec("slack_draft", "Slack draft queue",
                    "Prepare a reviewed Slack message and hold it for explicit send confirmation.", true, true),
            new ToolSpec("n8n_draft", "n8n draft queue",
                    "Prepare a reviewed n8n webhook payload and hold it for explicit execution confirmation.", true, true),
            new ToolSpec("telegram_webhook", "Telegram webhook",
                    "Send one reviewed message through a user-configured Telegram bot webhook after confirmation.", true, true),
            new ToolSpec("voice_session", "Voice conversation",
                    "Toggle continuous voice input mode; each utterance is transcribed and offered as a draft, never auto-sent.", false, false),
            new ToolSpec("arena_share", "Share arena result",
                    "Create a redacted, shareable summary card of a dual-model comparison (models, prompt, both answers).", true, false),
            new ToolSpec("sandbox_list", "Sandbox list",
                    "List files in the private app sandbox workspace.", false, false),
            new ToolSpec("sandbox_write", "Sandbox write file",
                    "Create or update one text file inside the private sandbox after confirmation.", true, false),
            new ToolSpec("sandbox_read", "Sandbox read file",
                    "Read one text file from the private sandbox.", false, false),
            new ToolSpec("sandbox_zip", "Sandbox zip",
                    "Zip sandbox files into one archive inside the sandbox after confirmation.", true, false),
            new ToolSpec("git_status", "Git status",
                    "Run a safe read-only git status in the local diagnostics environment.", false, false),
            new ToolSpec("git_log", "Git log",
                    "Show a short read-only commit log from the local diagnostics environment.", false, false),
            new ToolSpec("github_commit_file", "Commit file via API",
                    "Create a commit that adds/updates one file through the GitHub Contents API after explicit confirmation.", true, true),
            new ToolSpec("hermes_checkpoint", "Hermes checkpoint",
                    "Save a visible Plan/Process/Review checkpoint for the current Hermes run.", false, false),
            new ToolSpec("image_generate", "Generate image",
                    "Create an image from a text prompt via an OpenAI-compatible images API after user confirmation.", true, true),
            new ToolSpec("dev_loop_cycle", "Dev loop cycle",
                    "Run one Plan→Code→Test→Review→Edit→Debug cycle and report CONTINUE LOOP or DONE.", false, false),
            new ToolSpec("hermes_handoff", "Hermes handoff pack",
                    "Produce a final handoff summary with risks, next steps, and confirmation checklist.", false, false)
    ));

    private ToolRegistry() {
    }

    public static List<ToolSpec> all() {
        return TOOLS;
    }

    public static int writeCount() {
        int count = 0;
        for (ToolSpec tool : TOOLS) if (tool.isWriteTool()) count++;
        return count;
    }
}
