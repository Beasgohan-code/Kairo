package com.kairo.app.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight markdown styling for model output.
 * Keeps the dependency surface zero while delivering Claude/Groq-like readability:
 * bold, italic, headers, inline code, and fenced code blocks.
 */
public final class MarkdownRenderer {
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");

    // Soft Claude-inspired palette used only for rendering spans
    private static final int HEADER = Color.rgb(210, 200, 255);
    private static final int CODE_BG = Color.rgb(18, 20, 26);
    private static final int CODE_FG = Color.rgb(180, 230, 205);
    private static final int INLINE_CODE_BG = Color.rgb(42, 45, 56);

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
                // Skip the fence line itself from visible text for cleaner blocks
                if (index < lines.length - 1) {
                    // keep a single newline so spacing stays natural
                }
                continue;
            }

            int lineStart = result.length();
            result.append(line);

            // Headers
            if (trimmed.startsWith("### ")) {
                result.setSpan(new StyleSpan(Typeface.BOLD), lineStart, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new RelativeSizeSpan(1.05f), lineStart, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new ForegroundColorSpan(HEADER), lineStart, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (trimmed.startsWith("## ") || trimmed.startsWith("# ")) {
                result.setSpan(new StyleSpan(Typeface.BOLD), lineStart, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new RelativeSizeSpan(1.12f), lineStart, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new ForegroundColorSpan(HEADER), lineStart, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (index < lines.length - 1) {
                result.append('\n');
            }
        }

        if (codeBlock) {
            applyCodeBlock(result, codeStart, result.length());
        }

        // Inline styles (order matters: bold first so italic regex doesn't fight)
        applyInline(result, BOLD, true, false);
        applyInline(result, ITALIC, false, true);
        applyInline(result, INLINE_CODE, false, false);

        return result;
    }

    private static void applyCodeBlock(SpannableStringBuilder result, int start, int end) {
        if (start < 0 || start >= end) return;
        result.setSpan(new TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        result.setSpan(new BackgroundColorSpan(CODE_BG), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        result.setSpan(new ForegroundColorSpan(CODE_FG), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        result.setSpan(new RelativeSizeSpan(0.94f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static void applyInline(SpannableStringBuilder result, Pattern pattern, boolean bold, boolean italic) {
        // Work on a snapshot string so indices stay valid while we add spans
        String snapshot = result.toString();
        Matcher matcher = pattern.matcher(snapshot);
        while (matcher.find()) {
            int contentStart = matcher.start(1);
            int contentEnd = matcher.end(1);
            if (contentStart < 0 || contentEnd > result.length()) continue;

            if (bold) {
                result.setSpan(new StyleSpan(Typeface.BOLD), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (italic) {
                result.setSpan(new StyleSpan(Typeface.ITALIC), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                // inline code
                result.setSpan(new TypefaceSpan("monospace"), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new BackgroundColorSpan(INLINE_CODE_BG), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new RelativeSizeSpan(0.93f), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
