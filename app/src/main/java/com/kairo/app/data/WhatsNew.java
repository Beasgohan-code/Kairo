package com.kairo.app.data;

/** In-app release notes. Keep in sync with CHANGELOG.md. */
public final class WhatsNew {
    public static final String VERSION = "0.14.0";

    private WhatsNew() {
    }

    public static String currentVersion() {
        return VERSION;
    }

    public static String releaseNotes() {
        return "Kairo " + VERSION + "\n\n"
                + "Fixed\n"
                + "• Dev Loop was missing from the mode picker (ids were shifted)\n"
                + "• Fast/Deep and project instructions now always reach the model\n"
                + "• Markdown no longer leaves ** * ` markers in answers\n"
                + "• Pinned chats sort to the top of the sidebar\n"
                + "• Camera asks for permission before capture\n"
                + "• Voice commands no longer hijack ordinary dictation\n"
                + "• Exports use the full credential redactor\n\n"
                + "New\n"
                + "• Slash commands: /help /code /review /deep /file /summarize /skill-creator…\n"
                + "• Custom skills: compile a brief, review the card, wording only\n"
                + "• Find in chat, duplicate thread, edit last message\n"
                + "• Speak last answer (on-device TTS)\n"
                + "• Optional numeric PIN for app lock\n"
                + "• Composer draft survives app switches\n"
                + "• xAI Grok, Google Gemini, Hugging Face, Perplexity providers\n"
                + "• Copy last code fence from an answer\n"
                + "• In-app What’s new + feature ideas";
    }
}
