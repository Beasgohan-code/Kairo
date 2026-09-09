package com.kairo.app.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * App-lock PIN hashing. The PIN never leaves the device and is stored as salt + SHA-256.
 * This is an explicit unlock gate, not a substitute for full-disk encryption.
 */
public final class PinHasher {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PinHasher() {
    }

    public static boolean isValidPin(String pin) {
        if (pin == null) return false;
        if (pin.length() < 4 || pin.length() > 8) return false;
        for (int i = 0; i < pin.length(); i++) {
            if (!Character.isDigit(pin.charAt(i))) return false;
        }
        return true;
    }

    public static String newSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    public static String hash(String pin, String salt) {
        if (pin == null || salt == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(pin.getBytes(StandardCharsets.UTF_8));
            return toHex(digest.digest());
        } catch (Exception exception) {
            return "";
        }
    }

    public static boolean verify(String pin, String salt, String expectedHash) {
        if (expectedHash == null || expectedHash.isEmpty() || salt == null || salt.isEmpty()) {
            return false;
        }
        String actual = hash(pin, salt);
        if (actual.length() != expectedHash.length()) return false;
        int diff = 0;
        for (int i = 0; i < actual.length(); i++) {
            diff |= actual.charAt(i) ^ expectedHash.charAt(i);
        }
        return diff == 0;
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            out[i * 2] = HEX[value >>> 4];
            out[i * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(out);
    }
}
