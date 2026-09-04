package com.kairo.app.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Curated connector cards shown in the Connectors workspace. */
public final class ConnectorCatalog {
    private ConnectorCatalog() {
    }

    public static List<ConnectorDefinition> all() {
        return Collections.unmodifiableList(Arrays.asList(
                new ConnectorDefinition(
                        "github",
                        "GitHub",
                        "SHIP WITH CONTROL",
                        "Read repositories, issues, files, and prepare confirmed pushes or pull requests.",
                        "Repo summary · Issues · Files · Push · PR",
                        true,
                        true),
                new ConnectorDefinition(
                        "vercel",
                        "Vercel",
                        "DEPLOY WITHOUT LEAVING KAIRO",
                        "Inspect projects and deployments, open the dashboard, and trigger a reviewed deployment.",
                        "Projects · Deployments · Redeploy · Dashboard",
                        true,
                        true),
                new ConnectorDefinition(
                        "n8n",
                        "n8n",
                        "AUTOMATE THE HANDOFF",
                        "List workflows and executions, then run a configured webhook only after you approve the payload.",
                        "Workflows · Executions · Webhooks · Activate",
                        true,
                        true),
                new ConnectorDefinition(
                        "slack",
                        "Slack",
                        "KEEP THE TEAM IN LOOP",
                        "Read public channels and post a reviewed message through the Slack Web API.",
                        "Channels · Status · Post message",
                        true,
                        true),
                new ConnectorDefinition(
                        "notion",
                        "Notion",
                        "TURN NOTES INTO CONTEXT",
                        "Search workspace pages and bring selected notes into a Kairo conversation.",
                        "Search pages · Open source URL",
                        true,
                        false),
                new ConnectorDefinition(
                        "linear",
                        "Linear",
                        "TURN ISSUES INTO ACTION",
                        "Search assigned and workspace issues for planning context without changing issue state.",
                        "Test access · Search issues · Open source URL",
                        true,
                        false),
                new ConnectorDefinition(
                        "supabase",
                        "Supabase",
                        "INSPECT YOUR DATA LAYER",
                        "Read a bounded sample from a configured REST table without embedding a database SDK.",
                        "REST health · Table preview · Row limit",
                        true,
                        false),
                new ConnectorDefinition(
                        "discord",
                        "Discord webhook",
                        "SEND A REVIEWED UPDATE",
                        "Post a short status update to one configured Discord webhook after you approve it.",
                        "Webhook · Preview · Explicit send",
                        true,
                        true)
        ));
    }
}
