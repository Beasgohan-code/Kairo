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
                    "Send one reviewed status message through an encrypted webhook URL.", true, true)
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
