package com.kairo.app.core;

import java.io.IOException;
import java.net.SocketTimeoutException;

public final class NetworkErrors {
    private NetworkErrors() {
    }

    public static String friendly(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException) {
            return "The provider took too long to respond. Check your connection or try again.";
        }
        if (throwable instanceof IOException) {
            return "Could not reach the provider. Check the endpoint, network, and API key.";
        }
        String message = throwable == null ? "Unknown error" : throwable.getMessage();
        if (message == null || message.trim().isEmpty()) return "The request failed.";
        String lower = message.toLowerCase();
        if (lower.contains("http 401") || lower.contains("unauthorized")) {
            return "The provider rejected the API key (HTTP 401). Check Settings → Manage key.";
        }
        if (lower.contains("http 429") || lower.contains("rate limit")) {
            return "Rate limited (HTTP 429). Wait a moment or switch model.";
        }
        if (lower.contains("http 403") || lower.contains("forbidden")) {
            return "Access denied (HTTP 403). This key may lack permission for that model.";
        }
        if (lower.contains("http 404")) {
            return "The model or endpoint was not found (HTTP 404). Refresh the catalog or pick another model.";
        }
        return message;
    }
}
