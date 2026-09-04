package com.kairo.app.ui;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight markdown styling for model output without bringing a large rendering dependency. */
public final class MarkdownRenderer {
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");

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
            if (line.trim().startsWith("```")) {
                if (!codeBlock) {
                    codeBlock = true;
                    codeStart = result.length();
                } else {
                    applyCode(result, codeStart, result.length());
                    codeBlock = false;
                    codeStart = -1;
                }
            } else {
                int lineStart = result.length();
                result.append(line);
                if (line.startsWith("#")) {
                    result.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), lineStart,
                            result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    result.setSpan(new ForegroundColorSpan(Color.rgb(220, 209, 255)), lineStart,
                            result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (index < lines.length - 1) result.append('\n');
            }
        }
        if (codeBlock) applyCode(result, codeStart, result.length());
        applyInline(result, BOLD, true);
        applyInline(result, INLINE_CODE, false);
        return result;
    }

    private static void applyCode(SpannableStringBuilder result, int start, int end) {
        if (start < 0 || start >= end) return;
        result.setSpan(new TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        result.setSpan(new BackgroundColorSpan(Color.rgb(13, 14, 17)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        result.setSpan(new ForegroundColorSpan(Color.rgb(191, 235, 210)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static void applyInline(SpannableStringBuilder result, Pattern pattern, boolean bold) {
        Matcher matcher = pattern.matcher(result.toString());
        while (matcher.find()) {
            int contentStart = matcher.start(1);
            int contentEnd = matcher.end(1);
            result.setSpan(bold
                    ? new StyleSpan(android.graphics.Typeface.BOLD)
                    : new TypefaceSpan("monospace"), contentStart, contentEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (!bold) {
                result.setSpan(new BackgroundColorSpan(Color.rgb(46, 48, 57)), contentStart,
                        contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
