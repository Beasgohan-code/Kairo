package com.kairo.app.network;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minimal Slack Web API adapter with read actions and an explicit message-send action. */
public final class SlackClient {
    private static final String API_ROOT = "https://slack.com/api";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(String result);
        void onError(String message);
    }

    public void test(String token, Callback callback) {
        run(() -> {
            JSONObject root = request(token, "/auth.test", "GET", null);
            requireOk(root);
            return "Slack connected as " + root.optString("user", "workspace member")
                    + " in " + root.optString("team", "workspace") + ".";
        }, callback);
    }

    public void listChannels(String token, Callback callback) {
        run(() -> {
            String path = "/conversations.list?limit=50&exclude_archived=true&types="
                    + URLEncoder.encode("public_channel,private_channel", StandardCharsets.UTF_8.name());
            JSONObject root = request(token, path, "GET", null);
            requireOk(root);
            JSONArray channels = root.optJSONArray("channels");
            if (channels == null || channels.length() == 0) return "No Slack channels returned.";
            StringBuilder output = new StringBuilder("Slack channels\n");
            for (int index = 0; index < channels.length(); index++) {
                JSONObject channel = channels.optJSONObject(index);
                if (channel == null) continue;
                output.append("• #").append(channel.optString("name", "unnamed"))
                        .append("  ·  ").append(channel.optString("id", "no id"))
                        .append(channel.optBoolean("is_private", false) ? "  ·  private" : "")
                        .append('\n');
            }
            return output.toString().trim();
        }, callback);
    }

    public void sendMessage(String token, String channel, String message, Callback callback) {
        run(() -> {
            if (channel == null || !channel.trim().matches("[A-Za-z0-9_-]+")) {
                throw new IllegalArgumentException("Use a Slack channel id, for example C0123456789.");
            }
            String text = message == null ? "" : message.trim();
            if (text.isEmpty()) throw new IllegalArgumentException("Write a Slack message first.");
            if (text.length() > 4_000) throw new IllegalArgumentException("Slack messages are limited to 4,000 characters.");
            JSONObject body = new JSONObject();
            body.put("channel", channel.trim());
            body.put("text", text);
            JSONObject root = request(token, "/chat.postMessage", "POST", body);
            requireOk(root);
            return "Slack message sent to " + channel.trim() + ".";
        }, callback);
    }

    private void run(Work work, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                callback.onSuccess(work.run());
            } catch (Throwable throwable) {
                String message = throwable.getMessage();
                callback.onError(message == null || message.trim().isEmpty()
                        ? "Slack request failed." : message);
            }
        });
    }

    private interface Work {
        String run() throws Exception;
    }

    private JSONObject request(String token, String path, String method, JSONObject body) throws Exception {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Add a Slack bot token in Settings first.");
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_ROOT + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(45_000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token.trim());
            connection.setRequestProperty("User-Agent", "Kairo-Android/0.2");
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = read(stream);
            if (status < 200 || status >= 300) throw new IllegalStateException("Slack HTTP " + status + ": " + response);
            return new JSONObject(response);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void requireOk(JSONObject root) {
        if (!root.optBoolean("ok", false)) {
            throw new IllegalStateException("Slack: " + root.optString("error", "the API rejected the request"));
        }
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
