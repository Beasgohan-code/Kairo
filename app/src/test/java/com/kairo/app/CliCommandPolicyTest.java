package com.kairo.app;

import com.kairo.app.agent.CliCommandPolicy;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CliCommandPolicyTest {
    @Test
    public void allowsDiagnosticsOnly() {
        assertTrue(CliCommandPolicy.isAllowed("git status"));
        assertTrue(CliCommandPolicy.isAllowed("git log -5 --oneline"));
        assertTrue(CliCommandPolicy.isAllowed("uname -a"));
        assertFalse(CliCommandPolicy.isAllowed("git status && rm -rf ."));
        assertFalse(CliCommandPolicy.isAllowed("cat secret.txt"));
        assertFalse(CliCommandPolicy.isAllowed("$(whoami)"));
    }
}
