package com.kairo.app.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

/**
 * Lightweight markdown styling for model output.
 * Strips markers so **bold**, *italic*, and `code` render as styled text, not punctuation.
 */
public final class MarkdownRenderer {
    private static final int HEADER = Color.rgb(210, 200, 255);
    private static final int CODE_BG = Color.rgb(18, 20, 26);
    private static final int CODE_FG = Color.rgb(180, 230, 205);
    private static final int INLINE_CODE_BG = Color.rgb(42, 45, 56);
    private static final int LINK = Color.rgb(139, 180, 255);

    private MarkdownRenderer() {
    }

    public static Spanned render(String source) {
        String value = source == null ? "" : source;
        SpannableStringBuilder result = new SpannableStringBuilder();
        boolean codeBlock = false;
        int codeStart = -1;
        String[] lines = value.split("\\n", -1);

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            String trimmed = line.trim();

            if (trimmed.startsWith("```")) {
                if (!codeBlock) {
                    codeBlock = true;
                    codeStart = result.length();
                } else {
                    applyCodeBlock(result, codeStart, result.length());
                    codeBlock = false;
                    codeStart = -1;
                }
                continue;
            }

            if (codeBlock) {
                result.append(line);
            } else {
                appendStyledLine(result, line, trimmed);
            }

            if (index < lines.length - 1) {
                result.append('\n');
            }
        }

        if (codeBlock) {
            applyCodeBlock(result, codeStart, result.length());
        }

        return result;
    }

    /** Marker-stripped plain text, suitable for TTS and copy-without-markdown. */
    public static String plain(String source) {
        return render(source).toString();
    }

    private static void appendStyledLine(SpannableStringBuilder result, String line, String trimmed) {
        int lineStart = result.length();
        String working = line;

        int headerLevel = 0;
        if (trimmed.startsWith("### ")) headerLevel = 3;
        else if (trimmed.startsWith("## ")) headerLevel = 2;
        else if (trimmed.startsWith("# ")) headerLevel = 1;
        if (headerLevel > 0) {
            int hash = working.indexOf('#');
            int contentAt = hash;
            while (contentAt < working.length() && (working.charAt(contentAt) == '#' || working.charAt(contentAt) == ' ')) {
                contentAt++;
            }
            working = working.substring(0, hash) + working.substring(contentAt);
        }

        String listWorking = working.trim();
        boolean bullet = listWorking.startsWith("- ") || listWorking.startsWith("* ");
        boolean numbered = listWorking.matches("\\d+\\.\\s+.*");
        if (bullet) {
            int dash = working.indexOf(listWorking.charAt(0));
            working = working.substring(0, dash) + "• " + listWorking.substring(2);
        } else if (numbered) {
            int dot = listWorking.indexOf('.');
            String num = listWorking.substring(0, dot);
            working = working.substring(0, working.indexOf(num)) + num + ". " + listWorking.substring(dot + 1).trim();
        }

        SpannableStringBuilder inline = styleInline(working);
        result.append(inline);

        int lineEnd = result.length();
        if (lineEnd > lineStart && headerLevel > 0) {
            result.setSpan(new StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            result.setSpan(new RelativeSizeSpan(headerLevel == 1 ? 1.18f : (headerLevel == 2 ? 1.12f : 1.05f)),
                    lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            result.setSpan(new ForegroundColorSpan(HEADER), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static SpannableStringBuilder styleInline(String source) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        int i = 0;
        int n = source.length();
        while (i < n) {
            if (source.startsWith("**", i)) {
                int close = source.indexOf("**", i + 2);
                if (close > i + 2) {
                    int start = out.length();
                    out.append(styleInline(source.substring(i + 2, close)));
                    out.setSpan(new StyleSpan(Typeface.BOLD), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = close + 2;
                    continue;
                }
            }
            if (source.startsWith("~~", i)) {
                int close = source.indexOf("~~", i + 2);
                if (close > i + 2) {
                    int start = out.length();
                    out.append(styleInline(source.substring(i + 2, close)));
                    out.setSpan(new StrikethroughSpan(), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = close + 2;
                    continue;
                }
            }
            if (source.charAt(i) == '`' ) {
                int close = source.indexOf('`', i + 1);
                if (close > i + 1) {
                    int start = out.length();
                    out.append(source.substring(i + 1, close));
                    out.setSpan(new TypefaceSpan("monospace"), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new BackgroundColorSpan(INLINE_CODE_BG), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new RelativeSizeSpan(0.93f), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = close + 1;
                    continue;
                }
            }
            if (source.charAt(i) == '*' && (i + 1 >= n || source.charAt(i + 1) != '*')) {
                int close = indexOfSingleStar(source, i + 1);
                if (close > i + 1) {
                    int start = out.length();
                    out.append(styleInline(source.substring(i + 1, close)));
                    out.setSpan(new StyleSpan(Typeface.ITALIC), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = close + 1;
                    continue;
                }
            }
            if (source.charAt(i) == '[') {
                int bracket = source.indexOf(']', i + 1);
                if (bracket > i + 1 && bracket + 1 < n && source.charAt(bracket + 1) == '(') {
                    int paren = source.indexOf(')', bracket + 2);
                    if (paren > bracket + 2) {
                        String label = source.substring(i + 1, bracket);
                        String url = source.substring(bracket + 2, paren);
                        int start = out.length();
                        out.append(label);
                        out.append(" (");
                        out.append(url);
                        out.append(')');
                        out.setSpan(new ForegroundColorSpan(LINK), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        i = paren + 1;
                        continue;
                    }
                }
            }
            out.append(source.charAt(i));
            i++;
        }
        return out;
    }

    private static int indexOfSingleStar(String source, int from) {
        for (int i = from; i < source.length(); i++) {
            if (source.charAt(i) == '*' && (i + 1 >= source.length() || source.charAt(i + 1) != '*')) {
                return i;
            }
        }
        return -1;
    }

    private static void applyCodeBlock(SpannableStringBuilder result, int start, int end) {
        if (start < 0 || start >= end) return;
        result.setSpan(new TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        result.setSpan(new BackgroundColorSpan(CODE_BG), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        result.setSpan(new ForegroundColorSpan(CODE_FG), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        result.setSpan(new RelativeSizeSpan(0.94f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
