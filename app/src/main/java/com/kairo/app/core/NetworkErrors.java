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
        return message == null || message.trim().isEmpty() ? "The request failed." : message;
    }
}
