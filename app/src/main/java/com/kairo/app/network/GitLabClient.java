package com.kairo.app.network;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Read-only GitLab project list using a personal access token. */
public final class GitLabClient {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(String result);
        void onError(String message);
    }

    public void listProjects(String token, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                if (token == null || token.trim().isEmpty()) {
                    throw new IllegalArgumentException("Add a GitLab token first.");
                }
                HttpURLConnection c = (HttpURLConnection) new URL(
                        "https://gitlab.com/api/v4/projects?membership=true&per_page=20&order_by=last_activity_at")
                        .openConnection();
                c.setRequestMethod("GET");
                c.setRequestProperty("PRIVATE-TOKEN", token.trim());
                c.setRequestProperty("User-Agent", "Kairo-Android/0.10");
                c.setConnectTimeout(15_000);
                c.setReadTimeout(30_000);
                int status = c.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
                String body = read(stream);
                c.disconnect();
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("GitLab HTTP " + status + ": " + body);
                }
                JSONArray arr = new JSONArray(body);
                if (arr.length() == 0) {
                    callback.onSuccess("No GitLab projects found for this token.");
                    return;
                }
                StringBuilder out = new StringBuilder("GitLab projects\n");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject p = arr.optJSONObject(i);
                    if (p == null) continue;
                    out.append("• ").append(p.optString("path_with_namespace", p.optString("name")))
                            .append("  ·  ").append(p.optString("visibility", ""))
                            .append('\n');
                }
                callback.onSuccess(out.toString().trim());
            } catch (Exception e) {
                callback.onError(e.getMessage() == null ? "GitLab request failed" : e.getMessage());
            }
        });
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
