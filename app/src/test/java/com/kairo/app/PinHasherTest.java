package com.kairo.app;

import com.kairo.app.core.PinHasher;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PinHasherTest {
    @Test
    public void validatesAndVerifiesNumericPins() {
        assertTrue(PinHasher.isValidPin("1234"));
        assertTrue(PinHasher.isValidPin("12345678"));
        assertFalse(PinHasher.isValidPin("12"));
        assertFalse(PinHasher.isValidPin("123456789"));
        assertFalse(PinHasher.isValidPin("12ab"));
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash("2468", salt);
        assertTrue(PinHasher.verify("2468", salt, hash));
        assertFalse(PinHasher.verify("2469", salt, hash));
        assertNotEquals(hash, PinHasher.hash("2468", PinHasher.newSalt()));
    }
}
