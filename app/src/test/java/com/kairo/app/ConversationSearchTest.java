package com.kairo.app;

import com.kairo.app.core.ConversationSearch;
import com.kairo.app.data.ChatMessage;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConversationSearchTest {
    @Test
    public void findsCaseInsensitiveHitsWithSnippets() {
        List<ChatMessage> messages = Arrays.asList(
                new ChatMessage("user", "Please review the parser"),
                new ChatMessage("assistant", "The parser looks fine."));
        List<ConversationSearch.Hit> hits = ConversationSearch.search(messages, "PARSER");
        assertEquals(2, hits.size());
        assertEquals("user", hits.get(0).getRole());
        assertTrue(hits.get(0).label().startsWith("You"));
        assertTrue(hits.get(0).getSnippet().toLowerCase().contains("parser"));
        assertTrue(ConversationSearch.search(messages, "   ").isEmpty());
    }
}
