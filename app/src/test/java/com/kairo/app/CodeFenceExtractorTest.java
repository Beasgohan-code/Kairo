package com.kairo.app;

import com.kairo.app.core.CodeFenceExtractor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CodeFenceExtractorTest {
    @Test
    public void extractsLastFenceOrFallsBackToBody() {
        String answer = "intro\n```java\nclass A {}\n```\nthen\n```python\nprint(1)\n```\n";
        assertEquals(2, CodeFenceExtractor.count(answer));
        assertEquals("print(1)", CodeFenceExtractor.lastFence(answer));
        assertEquals("print(1)", CodeFenceExtractor.lastFenceOrBody(answer));
        assertEquals("plain body", CodeFenceExtractor.lastFenceOrBody("plain body"));
        assertEquals("", CodeFenceExtractor.lastFence("no fences here"));
        assertTrue(CodeFenceExtractor.all(answer).get(0).getLanguage().equals("java"));
    }
}
