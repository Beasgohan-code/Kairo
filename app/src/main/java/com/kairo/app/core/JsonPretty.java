package com.kairo.app.core;

/** Dependency-free JSON pretty-printer. Invalid JSON is returned unchanged. */
public final class JsonPretty {
    private JsonPretty() {
    }

    public static boolean looksLikeJson(String raw) {
        if (raw == null) return false;
        String trimmed = raw.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    public static String pretty(String raw) {
        if (raw == null) return "";
        String input = raw.trim();
        if (!looksLikeJson(input)) return raw;
        StringBuilder out = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (inString) {
                out.append(c);
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
            } else if (c == '{' || c == '[') {
                out.append(c);
                indent++;
                skipSpace(input, i);
                if (nextNonSpace(input, i + 1) != matchingClose(c)) {
                    out.append('\n');
                    pad(out, indent);
                }
            } else if (c == '}' || c == ']') {
                indent = Math.max(0, indent - 1);
                out.append('\n');
                pad(out, indent);
                out.append(c);
            } else if (c == ',') {
                out.append(c);
                out.append('\n');
                pad(out, indent);
                i = skipSpace(input, i);
            } else if (c == ':') {
                out.append(": ");
                i = skipSpace(input, i);
            } else if (!Character.isWhitespace(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static char matchingClose(char open) {
        return open == '{' ? '}' : ']';
    }

    private static int skipSpace(String input, int i) {
        int j = i + 1;
        while (j < input.length() && Character.isWhitespace(input.charAt(j))) j++;
        return j - 1;
    }

    private static char nextNonSpace(String input, int from) {
        for (int i = from; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!Character.isWhitespace(c)) return c;
        }
        return 0;
    }

    private static void pad(StringBuilder out, int indent) {
        for (int i = 0; i < indent; i++) out.append("  ");
    }
}
