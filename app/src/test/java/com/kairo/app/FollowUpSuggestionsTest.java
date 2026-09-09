package com.kairo.app;

import com.kairo.app.core.FollowUpSuggestions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FollowUpSuggestionsTest {
    @Test
    public void picksCodeErrorLongAndDefaultChips() {
        assertEquals("Explain this code step by step", FollowUpSuggestions.forAnswer("```java\nclass A {}\n```")[0]);
        assertEquals("How do I fix this?", FollowUpSuggestions.forAnswer("NullPointerException in production")[0]);
        StringBuilder longAnswer = new StringBuilder();
        while (longAnswer.length() < 650) longAnswer.append("word ");
        assertEquals("Summarize the key points", FollowUpSuggestions.forAnswer(longAnswer.toString())[0]);
        assertEquals("Go deeper on this", FollowUpSuggestions.forAnswer("short")[0]);
    }
}
