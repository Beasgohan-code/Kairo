package com.kairo.app.agent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Shell guardrail for the on-device CLI / Ubuntu-style sandbox terminal.
 * Rejects chaining, redirects, substitution, and anything outside a tight allow-list.
 * Not a full Ubuntu shell — diagnostics, read-only git, and light env probes only.
 */
public final class CliCommandPolicy {
    private static final Pattern DANGEROUS = Pattern.compile("[;&|<>`$\\n\\r]|\\$\\(");
    private static final List<String> EXAMPLES = Collections.unmodifiableList(Arrays.asList(
            "pwd",
            "ls",
            "ls -la",
            "ls -lah",
            "whoami",
            "id",
            "date",
            "uname -a",
            "hostname",
            "uptime",
            "df -h",
            "free -m",
            "cat /proc/version",
            "cat /proc/cpuinfo",
            "cat /proc/meminfo",
            "getprop ro.build.version.release",
            "getprop ro.product.model",
            "echo hello",
            "printf hello",
            "which sh",
            "which node",
            "which python3",
            "which java",
            "which clang",
            "which gcc",
            "node --version",
            "python3 --version",
            "java -version",
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
            "git show --stat HEAD",
            "sandbox-status",
            "help"
    ));

    private CliCommandPolicy() {
    }

    public static List<String> examples() {
        return EXAMPLES;
    }

    public static boolean isAllowed(String command) {
        if (command == null) return false;
        String value = command.trim();
        if (value.isEmpty() || value.length() > 200 || DANGEROUS.matcher(value).find()) return false;

        return value.matches("pwd")
                || value.matches("ls( -l(a|ah|ha)?)?")
                || value.matches("whoami")
                || value.matches("date")
                || value.matches("df -h")
                || value.matches("free -m")
                || value.matches("uname -a")
                || value.matches("hostname")
                || value.matches("uptime")
                || value.matches("id")
                || value.matches("help")
                || value.matches("sandbox-status")
                || value.matches("echo [A-Za-z0-9 _.,:+-]{1,80}")
                || value.matches("printf [A-Za-z0-9 _.,:+-]{1,80}")
                || value.matches("which (sh|node|deno|python3|java|javac|kotlinc|clang|clang\\+\\+|gcc|g\\+\\+|git)")
                || value.matches("(node|deno|python3|java|clang|gcc|git) --version")
                || value.matches("java -version")
                || value.matches("cat /proc/(version|cpuinfo|meminfo)")
                || value.matches("getprop ro\\.(build\\.version\\.release|product\\.model|product\\.manufacturer)")
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
        return "Outside the safe Ubuntu-style diagnostics allow-list. Use sandbox file tools for create/zip/run.";
    }

    /** Human help printed by the terminal `help` command. */
    public static String helpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Kairo sandbox terminal (Ubuntu-style diagnostics, not a full VM)\n");
        sb.append("Allowed examples:\n");
        for (String example : EXAMPLES) {
            sb.append("  · ").append(example).append('\n');
        }
        sb.append("\nThis is a tight allow-list. Blocked: pipes, redirects, chaining, substitution, root, package installs, arbitrary paths.");
        return sb.toString().trim();
    }
}
