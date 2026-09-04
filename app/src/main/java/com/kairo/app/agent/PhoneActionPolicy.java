package com.kairo.app.agent;

/**
 * Deliberately narrow, review-first phone surface.
 * Every action only opens a visible system screen after explicit confirmation.
 * No background control, no silent calls/messages, no root, no private-app access.
 */
public final class PhoneActionPolicy {
    private PhoneActionPolicy() { }

    public static boolean isSupported(String actionId) {
        if (actionId == null) return false;
        switch (actionId) {
            case "browser":
            case "settings":
            case "wifi":
            case "bluetooth":
            case "location":
            case "battery":
            case "display":
            case "sound":
            case "apps":
            case "camera":
            case "dialer":
            case "quick_settings":
                return true;
            default:
                return false;
        }
    }

    public static String boundary() {
        return "Only opens a visible Android screen after your explicit confirmation. "
                + "Cannot root the phone, place calls or send messages silently, read private apps, "
                + "run arbitrary shell, or control the device in the background.";
    }

    public static String description(String actionId) {
        if (actionId == null) return "";
        switch (actionId) {
            case "browser": return "Open the default browser";
            case "settings": return "Open system Settings";
            case "wifi": return "Open Wi-Fi settings";
            case "bluetooth": return "Open Bluetooth settings";
            case "location": return "Open Location settings";
            case "battery": return "Open Battery / Power settings";
            case "display": return "Open Display settings";
            case "sound": return "Open Sound settings";
            case "apps": return "Open App info / installed apps";
            case "camera": return "Open the camera app";
            case "dialer": return "Open the dialer with an optional number";
            case "quick_settings": return "Open Quick Settings panel (if supported)";
            default: return actionId;
        }
    }
}
