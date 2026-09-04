package com.kairo.app.agent;

/** Documents the deliberately narrow phone surface exposed by Kairo. */
public final class PhoneActionPolicy {
    private PhoneActionPolicy() { }

    public static boolean isSupported(String actionId) {
        return "browser".equals(actionId)
                || "settings".equals(actionId)
                || "wifi".equals(actionId)
                || "camera".equals(actionId)
                || "dialer".equals(actionId);
    }

    public static String boundary() {
        return "Kairo only opens a visible Android screen after your confirmation. It cannot root the phone, silently call or message, read private apps, run arbitrary shell commands, or control the device in the background.";
    }
}
