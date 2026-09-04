package com.kairo.app.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Local device profile used by the setup screen. Kairo has no hosted account server: the profile
 * gives the installation a stable label and a private setup state without collecting identity.
 */
public final class DeviceSetupStore {
    private static final String PREFS = "kairo_device_setup";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_PAIRING_CODE = "pairing_code";
    private static final String KEY_COMPLETE = "setup_complete";

    private final SharedPreferences preferences;

    public DeviceSetupStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureDeviceIdentity();
    }

    public String getDeviceId() {
        return preferences.getString(KEY_DEVICE_ID, "unknown-device");
    }

    public String getDeviceName() {
        return preferences.getString(KEY_DEVICE_NAME, "My Kairo device");
    }

    public void setDeviceName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) value = "My Kairo device";
        if (value.length() > 48) value = value.substring(0, 48).trim();
        preferences.edit().putString(KEY_DEVICE_NAME, value).apply();
    }

    /** A local pairing label, not an authentication credential or a network login token. */
    public String getPairingCode() {
        return preferences.getString(KEY_PAIRING_CODE, "------");
    }

    public boolean isSetupComplete() {
        return preferences.getBoolean(KEY_COMPLETE, false);
    }

    public void markSetupComplete(boolean complete) {
        preferences.edit().putBoolean(KEY_COMPLETE, complete).apply();
    }

    private void ensureDeviceIdentity() {
        if (preferences.contains(KEY_DEVICE_ID) && preferences.contains(KEY_PAIRING_CODE)) return;
        String deviceId = UUID.randomUUID().toString().replace("-", "");
        SecureRandom random = new SecureRandom();
        String pairingCode = String.format("%06d", random.nextInt(1_000_000));
        preferences.edit()
                .putString(KEY_DEVICE_ID, deviceId)
                .putString(KEY_PAIRING_CODE, pairingCode)
                .apply();
    }
}
