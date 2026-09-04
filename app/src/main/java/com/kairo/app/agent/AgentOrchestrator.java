package com.kairo.app.agent;

import com.kairo.app.data.AgentDefinition;
import com.kairo.app.data.AgentCatalog;

import java.util.List;

/**
 * Central registry for agent capabilities. Keeping this small makes it easy to add a new
 * explicit tool without giving an LLM arbitrary device or GitHub access.
 */
public final class AgentOrchestrator {
    private final CliAgent cliAgent;
    private final CodeRunner codeRunner;

    public AgentOrchestrator() {
        cliAgent = new CliAgent();
        codeRunner = new CodeRunner();
    }

    public List<AgentDefinition> agents() {
        return AgentCatalog.all();
    }

    public List<ToolSpec> tools() {
        return ToolRegistry.all();
    }

    public CliAgent cli() {
        return cliAgent;
    }

    public CodeRunner codeRunner() {
        return codeRunner;
    }

    public boolean supports(String agentId) {
        return "code".equals(agentId)
                || "hermes".equals(agentId)
                || "devloop".equals(agentId)
                || "github".equals(agentId)
                || "cli".equals(agentId)
                || "phone".equals(agentId)
                || "research".equals(agentId)
                || "artifact".equals(agentId)
                || "browser".equals(agentId)
                || "automation".equals(agentId)
                || "arena".equals(agentId);
    }
}
