package com.kairo.app.network;

import org.json.JSONArray;
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

/** Search-only Notion connector for deliberately selected workspace context. */
public final class NotionClient {
    private static final String API_ROOT = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(String result);
        void onError(String message);
    }

    public void search(String token, String query, Callback callback) {
        run(() -> {
            String cleanQuery = query == null ? "" : query.trim();
            if (cleanQuery.length() > 200) throw new IllegalArgumentException("Notion searches are limited to 200 characters.");
            JSONObject body = new JSONObject();
            if (!cleanQuery.isEmpty()) body.put("query", cleanQuery);
            body.put("page_size", 20);
            JSONObject root = request(token, "/search", "POST", body);
            JSONArray results = root.optJSONArray("results");
            if (results == null || results.length() == 0) return "No matching Notion pages found.";
            StringBuilder output = new StringBuilder("Notion pages\n");
            for (int index = 0; index < results.length(); index++) {
                JSONObject result = results.optJSONObject(index);
                if (result == null) continue;
                output.append("• ").append(titleOf(result))
                        .append("\n  ").append(result.optString("url", "No page URL"))
                        .append('\n');
            }
            return output.toString().trim();
        }, callback);
    }

    private static String titleOf(JSONObject page) {
        JSONObject properties = page.optJSONObject("properties");
        if (properties != null) {
            JSONArray names = properties.names();
            if (names != null) {
                for (int index = 0; index < names.length(); index++) {
                    JSONObject property = properties.optJSONObject(names.optString(index));
                    if (property == null) continue;
                    JSONArray title = property.optJSONArray("title");
                    if (title == null) title = property.optJSONArray("rich_text");
                    String value = richText(title);
                    if (!value.isEmpty()) return value;
                }
            }
        }
        JSONObject parentTitle = page.optJSONObject("title");
        String value = richText(parentTitle == null ? null : parentTitle.optJSONArray("title"));
        return value.isEmpty() ? "Untitled Notion page" : value;
    }

    private static String richText(JSONArray parts) {
        if (parts == null) return "";
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < parts.length(); index++) {
            JSONObject part = parts.optJSONObject(index);
            if (part != null) result.append(part.optString("plain_text", part.optString("text", "")));
        }
        return result.toString().trim();
    }

    private void run(Work work, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                callback.onSuccess(work.run());
            } catch (Throwable throwable) {
                String message = throwable.getMessage();
                callback.onError(message == null || message.trim().isEmpty()
                        ? "Notion request failed." : message);
            }
        });
    }

    private interface Work {
        String run() throws Exception;
    }

    private JSONObject request(String token, String path, String method, JSONObject body) throws Exception {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Add a Notion integration token in Settings first.");
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_ROOT + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(45_000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + token.trim());
            connection.setRequestProperty("Notion-Version", NOTION_VERSION);
            connection.setRequestProperty("User-Agent", "Kairo-Android/0.2");
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = read(stream);
            if (status < 200 || status >= 300) throw new IllegalStateException("Notion HTTP " + status + ": " + response);
            return new JSONObject(response);
        } finally {
            if (connection != null) connection.disconnect();
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
