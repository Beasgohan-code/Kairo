package com.kairo.app;

import com.kairo.app.core.UnifiedDiff;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnifiedDiffTest {
    @Test
    public void reportsChangedLinesAndEmptyDiffs() {
        String diff = UnifiedDiff.diff("a\nb\nc", "a\nB\nc");
        assertTrue(diff.contains("- b"));
        assertTrue(diff.contains("+ B"));
        assertFalse(UnifiedDiff.isEmpty(diff));
        assertTrue(UnifiedDiff.isEmpty(UnifiedDiff.diff("same", "same")));
        assertEquals("(no line differences detected)", UnifiedDiff.diff(null, null));
    }
}
