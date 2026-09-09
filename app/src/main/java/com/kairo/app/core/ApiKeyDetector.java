package com.kairo.app.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects common provider credential formats without logging or returning them to analytics. */
public final class ApiKeyDetector {
    private static final Rule[] RULES = {
            new Rule("github", "GitHub", Pattern.compile("\\b(ghp_[A-Za-z0-9]{20,})\\b")),
            new Rule("github", "GitHub", Pattern.compile("\\b(github_pat_[A-Za-z0-9_]{20,})\\b")),
            new Rule("github", "GitHub", Pattern.compile("\\b(gho_[A-Za-z0-9]{20,})\\b")),
            new Rule("github", "GitHub", Pattern.compile("\\b(ghu_[A-Za-z0-9]{20,})\\b")),
            new Rule("github", "GitHub", Pattern.compile("\\b(ghs_[A-Za-z0-9]{20,})\\b")),
            new Rule("github", "GitHub", Pattern.compile("\\b(ghr_[A-Za-z0-9]{20,})\\b")),
            new Rule("anthropic", "Anthropic", Pattern.compile("\\b(sk-ant-[A-Za-z0-9_-]{20,})\\b")),
            new Rule("openrouter", "OpenRouter", Pattern.compile("\\b(sk-or-v1-[A-Za-z0-9_-]{20,})\\b")),
            new Rule("groq", "Groq", Pattern.compile("\\b(gsk_[A-Za-z0-9_-]{20,})\\b")),
            new Rule("nvidia", "NVIDIA", Pattern.compile("\\b(nvapi[-_][A-Za-z0-9_-]{20,})\\b")),
            new Rule("openai", "OpenAI", Pattern.compile("\\b(sk-proj-[A-Za-z0-9_-]{20,})\\b")),
            new Rule("openai", "OpenAI", Pattern.compile("\\b(sk-svcacct-[A-Za-z0-9_-]{20,})\\b")),
            new Rule("huggingface", "Hugging Face", Pattern.compile("\\b(hf_[A-Za-z0-9]{20,})\\b")),
            new Rule("xai", "xAI", Pattern.compile("\\b(xai-[A-Za-z0-9_-]{20,})\\b")),
            new Rule("perplexity", "Perplexity", Pattern.compile("\\b(pplx-[A-Za-z0-9_-]{20,})\\b")),
            new Rule("google", "Google AI", Pattern.compile("\\b(AIza[0-9A-Za-z_-]{20,})\\b")),
            new Rule("linear", "Linear", Pattern.compile("\\b(lin_api_[A-Za-z0-9_-]{20,})\\b")),
            new Rule("slack", "Slack", Pattern.compile("\\b(xox[baprs]-[A-Za-z0-9-]{20,})\\b")),
            new Rule("notion", "Notion", Pattern.compile("\\b(secret_[A-Za-z0-9]{20,})\\b"))
    };

    private ApiKeyDetector() {
    }

    public static DetectedCredential detect(String text) {
        if (text == null || text.isEmpty()) return null;
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern.matcher(text);
            if (matcher.find()) {
                String value = matcher.group(1);
                if (value != null && !value.trim().isEmpty()) {
                    return new DetectedCredential(rule.providerId, rule.providerName, value);
                }
            }
        }
        return null;
    }

    public static String redact(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        String result = text;
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern.matcher(result);
            StringBuffer output = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(output, Matcher.quoteReplacement("[" + rule.providerName + " key redacted]"));
            }
            matcher.appendTail(output);
            result = output.toString();
        }
        return result;
    }

    public static final class DetectedCredential {
        private final String providerId;
        private final String providerName;
        private final String value;

        private DetectedCredential(String providerId, String providerName, String value) {
            this.providerId = providerId;
            this.providerName = providerName;
            this.value = value;
        }

        public String getProviderId() {
            return providerId;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getValue() {
            return value;
        }

        public String masked() {
            int visible = Math.min(4, value.length());
            return value.substring(0, visible) + "••••••";
        }
    }

    private static final class Rule {
        private final String providerId;
        private final String providerName;
        private final Pattern pattern;

        private Rule(String providerId, String providerName, Pattern pattern) {
            this.providerId = providerId;
            this.providerName = providerName;
            this.pattern = pattern;
        }
    }
}
