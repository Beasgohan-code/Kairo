package com.kairo.app.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Stores provider credentials encrypted with an AES key held by Android Keystore.
 * The ciphertext is kept in private app preferences; plain API keys never leave this class.
 */
public final class ApiKeyStore {
    private static final String TAG = "KairoApiKeyStore";
    private static final String PREFS = "kairo_secret_values";
    private static final String KEY_ALIAS = "kairo_api_keys_v1";
    private static final String VALUE_SEPARATOR = ":";

    private final SharedPreferences preferences;

    public ApiKeyStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void save(String providerId, String apiKey) {
        if (providerId == null || providerId.trim().isEmpty()) {
            return;
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            delete(providerId);
            return;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] encrypted = cipher.doFinal(apiKey.trim().getBytes(StandardCharsets.UTF_8));
            String value = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
                    + VALUE_SEPARATOR
                    + Base64.encodeToString(encrypted, Base64.NO_WRAP);
            preferences.edit().putString(providerKey(providerId), value).apply();
        } catch (Exception exception) {
            Log.e(TAG, "Could not encrypt a provider key", exception);
            throw new IllegalStateException("Secure key storage is unavailable", exception);
        }
    }

    public synchronized String get(String providerId) {
        if (providerId == null) {
            return "";
        }
        String value = preferences.getString(providerKey(providerId), "");
        if (value.isEmpty()) {
            return "";
        }
        try {
            String[] pieces = value.split(VALUE_SEPARATOR, 2);
            if (pieces.length != 2) {
                return "";
            }
            byte[] iv = Base64.decode(pieces[0], Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(pieces[1], Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            // A key can become invalid after a device restore. Do not expose ciphertext or fail
            // the whole app; the user can simply enter the provider key again.
            Log.w(TAG, "Could not decrypt a provider key", exception);
            return "";
        }
    }

    public synchronized boolean hasKey(String providerId) {
        return !get(providerId).isEmpty();
    }

    public synchronized void delete(String providerId) {
        if (providerId != null) {
            preferences.edit().remove(providerKey(providerId)).apply();
        }
    }

    private String providerKey(String providerId) {
        return "provider_" + providerId.trim().toLowerCase();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            java.security.Key key = keyStore.getKey(KEY_ALIAS, null);
            if (key instanceof SecretKey) {
                return (SecretKey) key;
            }
        }
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
