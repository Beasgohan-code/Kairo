package com.kairo.app.network;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Sends a bounded text update to one explicitly configured Discord webhook. */
public final class DiscordWebhookClient {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(String result);
        void onError(String message);
    }

    public void send(String webhookUrl, String message, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                if (webhookUrl == null || !webhookUrl.trim().startsWith("https://")) {
                    throw new IllegalArgumentException("Use an HTTPS Discord webhook URL.");
                }
                String text = message == null ? "" : message.trim();
                if (text.isEmpty()) throw new IllegalArgumentException("Write a Discord message first.");
                if (text.length() > 2_000) throw new IllegalArgumentException("Discord messages are limited to 2,000 characters.");
                JSONObject body = new JSONObject();
                body.put("content", text);
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(webhookUrl.trim()).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(15_000);
                    connection.setReadTimeout(45_000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    int status = connection.getResponseCode();
                    String response = read(status >= 200 && status < 300
                            ? connection.getInputStream() : connection.getErrorStream());
                    if (status < 200 || status >= 300) throw new IllegalStateException("Discord HTTP " + status + ": " + response);
                    callback.onSuccess("Discord webhook accepted." + (response.isEmpty() ? "" : "\n" + response));
                } finally {
                    if (connection != null) connection.disconnect();
                }
            } catch (Throwable throwable) {
                String messageText = throwable.getMessage();
                callback.onError(messageText == null || messageText.trim().isEmpty()
                        ? "Discord webhook failed." : messageText);
            }
        });
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }
}
