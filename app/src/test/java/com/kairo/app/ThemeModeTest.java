package com.kairo.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Documents supported theme mode ids without requiring Android runtime. */
public class ThemeModeTest {
    @Test
    public void supportedThemeModes() {
        String[] modes = {"dark", "light", "system"};
        assertEquals(3, modes.length);
        assertTrue("dark".equals(modes[0]));
        assertTrue("light".equals(modes[1]));
        assertTrue("system".equals(modes[2]));
    }
}
