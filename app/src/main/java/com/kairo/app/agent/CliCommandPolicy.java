package com.kairo.app.agent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Shell guardrail for the on-device CLI agent. It intentionally rejects chaining, redirects,
 * substitution, environment expansion, and commands outside a tiny diagnostics allow-list.
 */
public final class CliCommandPolicy {
    private static final Pattern DANGEROUS = Pattern.compile("[;&|<>`$\\n\\r]|\\$\\(");
    private static final List<String> EXAMPLES = Collections.unmodifiableList(Arrays.asList(
            "pwd",
            "ls",
            "ls -la",
            "git status",
            "git diff --stat",
            "git branch --show-current",
            "git log -5 --oneline",
            "uname -a",
            "id"
    ));

    private CliCommandPolicy() {
    }

    public static List<String> examples() {
        return EXAMPLES;
    }

    public static boolean isAllowed(String command) {
        if (command == null) return false;
        String value = command.trim();
        if (value.isEmpty() || value.length() > 160 || DANGEROUS.matcher(value).find()) return false;
        return value.matches("pwd")
                || value.matches("ls( -la)?")
                || value.matches("git status")
                || value.matches("git diff --stat")
                || value.matches("git branch --show-current")
                || value.matches("git log -[1-9][0-9]? --oneline")
                || value.matches("uname -a")
                || value.matches("id");
    }

    public static String rejectionReason(String command) {
        if (command == null || command.trim().isEmpty()) return "Enter a command.";
        if (DANGEROUS.matcher(command).find()) return "Pipes, redirects, chaining, and substitution are blocked.";
        return "That command is outside Kairo's safe diagnostics allow-list.";
    }
}
