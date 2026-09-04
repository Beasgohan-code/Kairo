package com.kairo.app;

import com.kairo.app.agent.PhoneActionPolicy;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PhoneActionPolicyTest {
    @Test
    public void exposesOnlyVisibleIntentActions() {
        assertTrue(PhoneActionPolicy.isSupported("browser"));
        assertTrue(PhoneActionPolicy.isSupported("dialer"));
        assertFalse(PhoneActionPolicy.isSupported("sms"));
        assertFalse(PhoneActionPolicy.isSupported("root-shell"));
    }
}
