package com.kairo.app;

import com.kairo.app.core.JsonPretty;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonPrettyTest {
    @Test
    public void prettyPrintsObjectsAndLeavesNonJsonAlone() {
        assertTrue(JsonPretty.looksLikeJson("{\"a\":1}"));
        assertFalse(JsonPretty.looksLikeJson("not json"));
        String pretty = JsonPretty.pretty("{\"a\":1,\"b\":[2,3]}");
        assertTrue(pretty.contains("\n"));
        assertTrue(pretty.contains("\"a\": 1"));
        assertEquals("hello", JsonPretty.pretty("hello"));
        assertEquals("", JsonPretty.pretty(null));
    }
}
