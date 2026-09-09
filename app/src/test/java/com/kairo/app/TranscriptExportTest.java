package com.kairo.app;

import com.kairo.app.core.TranscriptExport;
import com.kairo.app.data.ChatMessage;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TranscriptExportTest {
    @Test
    public void redactsCredentialsInMarkdownJsonAndPlain() {
        ChatMessage user = new ChatMessage("user", "my key is sk-ant-abcdefghijklmnopqrstuvwxyz123456");
        ChatMessage assistant = new ChatMessage("assistant", "ok");
        String md = TranscriptExport.markdown("Secret thread", Arrays.asList(user, assistant), "keep secrets out");
        assertTrue(md.contains("# Secret thread"));
        assertTrue(md.contains("### You"));
        assertTrue(md.contains("[Anthropic key redacted]"));
        assertFalse(md.contains("sk-ant-abcdefghijklmnopqrstuvwxyz123456"));
        assertTrue(md.contains("Project instructions"));

        String json = TranscriptExport.json("t", Arrays.asList(user));
        assertTrue(json.contains("\"redacted\": true"));
        assertFalse(json.contains("sk-ant-abcdefghijklmnopqrstuvwxyz123456"));

        String plain = TranscriptExport.sharePlain("t", Arrays.asList(user, assistant));
        assertTrue(plain.contains("You:"));
        assertTrue(plain.contains("Kairo:"));
    }
}
