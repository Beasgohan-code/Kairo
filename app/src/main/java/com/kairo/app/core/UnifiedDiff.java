package com.kairo.app.core;

/**
 * Small line-oriented unified diff for review dialogs.
 * Not a full Myers implementation — enough to show what will change before a confirm.
 */
public final class UnifiedDiff {
    private UnifiedDiff() {
    }

    public static String diff(String before, String after) {
        return diff(before, after, 80);
    }

    public static String diff(String before, String after, int maxChangedLines) {
        String left = before == null ? "" : before;
        String right = after == null ? "" : after;
        if (left.equals(right)) return "(no line differences detected)";

        String[] oldLines = left.split("\n", -1);
        String[] newLines = right.split("\n", -1);
        StringBuilder out = new StringBuilder();
        out.append("--- before\n+++ after\n");
        int shown = 0;
        int limit = Math.max(1, maxChangedLines);
        int max = Math.max(oldLines.length, newLines.length);
        for (int i = 0; i < max; i++) {
            String a = i < oldLines.length ? oldLines[i] : null;
            String b = i < newLines.length ? newLines[i] : null;
            if (a != null && b != null && a.equals(b)) continue;
            if (shown >= limit) {
                out.append("… (").append(max - i).append(" more lines)\n");
                break;
            }
            if (a != null && (b == null || !a.equals(b))) {
                out.append("- ").append(a).append('\n');
                shown++;
                if (shown >= limit) {
                    out.append("…\n");
                    break;
                }
            }
            if (b != null && (a == null || !a.equals(b))) {
                out.append("+ ").append(b).append('\n');
                shown++;
            }
        }
        if (shown == 0) return "(no line differences detected)";
        return out.toString().trim();
    }

    public static boolean isEmpty(String diff) {
        return diff == null || diff.isEmpty() || diff.startsWith("(no line");
    }
}
