package com.kairo.app.agent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Shell guardrail for the on-device CLI agent.
 * Rejects chaining, redirects, substitution, and anything outside a tight allow-list.
 * Not a full Ubuntu shell — diagnostics + read-only git only.
 */
public final class CliCommandPolicy {
    private static final Pattern DANGEROUS = Pattern.compile("[;&|<>`$\\n\\r]|\\$\\(");
    private static final List<String> EXAMPLES = Collections.unmodifiableList(Arrays.asList(
            "pwd",
            "ls",
            "ls -la",
            "git status",
            "git status -sb",
            "git diff --stat",
            "git diff --name-only",
            "git branch --show-current",
            "git branch -a",
            "git log -5 --oneline",
            "git log -10 --oneline",
            "git remote -v",
            "git rev-parse --short HEAD",
            "uname -a",
            "id",
            "whoami",
            "date",
            "df -h",
            "env | head"
    ));

    private CliCommandPolicy() {
    }

    public static List<String> examples() {
        return EXAMPLES;
    }

    public static boolean isAllowed(String command) {
        if (command == null) return false;
        String value = command.trim();
        if (value.isEmpty() || value.length() > 180 || DANGEROUS.matcher(value).find()) return false;

        // Note: "env | head" is listed as example text only; pipes are blocked by DANGEROUS.
        return value.matches("pwd")
                || value.matches("ls( -la)?")
                || value.matches("whoami")
                || value.matches("date")
                || value.matches("df -h")
                || value.matches("uname -a")
                || value.matches("id")
                || value.matches("git status")
                || value.matches("git status -sb")
                || value.matches("git diff --stat")
                || value.matches("git diff --name-only")
                || value.matches("git branch --show-current")
                || value.matches("git branch -a")
                || value.matches("git log -[1-9][0-9]? --oneline")
                || value.matches("git remote -v")
                || value.matches("git rev-parse --short HEAD")
                || value.matches("git show --stat HEAD");
    }

    public static String rejectionReason(String command) {
        if (command == null || command.trim().isEmpty()) return "Enter a command.";
        if (DANGEROUS.matcher(command).find()) {
            return "Pipes, redirects, chaining, and substitution are blocked for safety.";
        }
        return "Outside the safe diagnostics allow-list. Use sandbox file tools for create/zip.";
    }
}
