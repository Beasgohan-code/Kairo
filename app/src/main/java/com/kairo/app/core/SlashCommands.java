package com.kairo.app.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Composer slash commands. Parsed on-device before a request is sent.
 * Unknown commands are left untouched so a literal "/foo" can still be a prompt.
 */
public final class SlashCommands {
    public static final class Result {
        private final boolean matched;
        private final boolean consumeOnly;
        private final String prompt;
        private final String agentId;
        private final String reasoningMode;
        private final String helpTopic;

        private Result(boolean matched, boolean consumeOnly, String prompt,
                       String agentId, String reasoningMode, String helpTopic) {
            this.matched = matched;
            this.consumeOnly = consumeOnly;
            this.prompt = prompt == null ? "" : prompt;
            this.agentId = agentId;
            this.reasoningMode = reasoningMode;
            this.helpTopic = helpTopic;
        }

        public boolean isMatched() {
            return matched;
        }

        public boolean isConsumeOnly() {
            return consumeOnly;
        }

        public String getPrompt() {
            return prompt;
        }

        public String getAgentId() {
            return agentId;
        }

        public String getReasoningMode() {
            return reasoningMode;
        }

        public String getHelpTopic() {
            return helpTopic;
        }
    }

    private static final Map<String, String> COMMANDS = new LinkedHashMap<>();

    static {
        COMMANDS.put("/help", "Show slash-command help");
        COMMANDS.put("/fast", "Switch to Fast reasoning");
        COMMANDS.put("/balanced", "Switch to Balanced reasoning");
        COMMANDS.put("/deep", "Switch to Deep reasoning");
        COMMANDS.put("/code", "Code agent · plan and implement");
        COMMANDS.put("/review", "Review code for bugs and edge cases");
        COMMANDS.put("/test", "Generate unit tests");
        COMMANDS.put("/explain", "Explain code step by step");
        COMMANDS.put("/fix", "Propose a focused fix");
        COMMANDS.put("/security", "Security and privacy review");
        COMMANDS.put("/json", "Extract structured JSON");
        COMMANDS.put("/file", "Create a complete file artifact");
        COMMANDS.put("/summarize", "Summarize this conversation");
        COMMANDS.put("/skill-creator", "Compile a custom skill from a brief");
        COMMANDS.put("/skills", "Open Skills & language");
        COMMANDS.put("/arena", "Open dual-model Arena");
        COMMANDS.put("/search", "Open web search");
    }

    private SlashCommands() {
    }

    public static Result none() {
        return new Result(false, false, "", null, null, null);
    }

    public static Result parse(String raw) {
        if (raw == null) return none();
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.charAt(0) != '/') return none();

        int split = -1;
        for (int i = 1; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == ' ' || c == '\n' || c == '\t') {
                split = i;
                break;
            }
        }
        String cmd = (split < 0 ? trimmed : trimmed.substring(0, split)).toLowerCase(Locale.US);
        String rest = split < 0 ? "" : trimmed.substring(split).trim();

        switch (cmd) {
            case "/help":
            case "/?":
                return new Result(true, true, "", null, null, "help");
            case "/fast":
                return reasoning("fast", rest);
            case "/balanced":
                return reasoning("balanced", rest);
            case "/deep":
            case "/think":
                return reasoning("deep", rest);
            case "/code":
                return agent("code", rest.isEmpty()
                        ? "Start with a plan, list assumptions, and give patch-ready steps for: "
                        : rest, false);
            case "/review":
                return agent("code", prefixed(
                        "Review the following code for bugs, edge cases, performance issues, and maintainability. Give fixes with rationale:",
                        rest), false);
            case "/test":
                return agent("code", prefixed(
                        "Generate focused unit and integration tests for the following. Include edge cases and how to run them:",
                        rest), false);
            case "/explain":
                return agent("code", prefixed(
                        "Explain the following code step by step, including inputs, outputs, assumptions, and likely failure modes:",
                        rest), false);
            case "/fix":
                return agent("code", prefixed(
                        "Propose the smallest correct fix for the following. Show the patch and why it is safe:",
                        rest), false);
            case "/security":
                return agent("code", prefixed(
                        "Review the following for security, privacy, secret leakage, unsafe permissions, and injection risks:",
                        rest), false);
            case "/json":
                return agent("chat", prefixed(
                        "Extract the useful facts from the following into valid JSON. Return JSON only:",
                        rest), false);
            case "/file":
                return agent("artifact", rest.isEmpty()
                        ? "Create a complete production-ready file. Suggest a safe filename and language, then return the entire file in one fenced code block. Requirements: "
                        : rest, false);
            case "/summarize":
                return agent("chat",
                        "Summarize this conversation with decisions, open questions, risks, and the next practical steps:",
                        false);
            case "/skill-creator":
            case "/skillcreator":
            case "/new-skill":
            case "/skill":
                return new Result(true, true, rest, null, null, "skill-creator");
            case "/skills":
                return new Result(true, true, "", null, null, "skills");
            case "/arena":
                return new Result(true, true, rest, null, null, "arena");
            case "/search":
                return new Result(true, true, rest, null, null, "search");
            default:
                return none();
        }
    }

    public static boolean isKnownCommand(String raw) {
        return parse(raw).isMatched();
    }

    public static List<String> commandNames() {
        return Collections.unmodifiableList(Arrays.asList(COMMANDS.keySet().toArray(new String[0])));
    }

    public static String helpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Slash commands (parsed on this device, never sent as a secret):\n");
        for (Map.Entry<String, String> entry : COMMANDS.entrySet()) {
            sb.append("  ").append(entry.getKey());
            for (int i = entry.getKey().length(); i < 16; i++) sb.append(' ');
            sb.append(entry.getValue()).append('\n');
        }
        sb.append("\n/skill-creator <brief>  compiles a reviewable skill. It never grants tools.");
        sb.append("\nUnknown /commands are sent as normal text.");
        return sb.toString().trim();
    }

    private static Result reasoning(String mode, String rest) {
        if (rest.isEmpty()) {
            return new Result(true, true, "", null, mode, null);
        }
        return new Result(true, false, rest, null, mode, null);
    }

    private static Result agent(String agentId, String prompt, boolean consumeOnly) {
        return new Result(true, consumeOnly, prompt, agentId, null, null);
    }

    private static String prefixed(String instruction, String rest) {
        if (rest == null || rest.isEmpty()) return instruction;
        return instruction + "\n\nInput:\n" + rest;
    }
}
