package com.kairo.app.network;

import android.util.Base64;

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

/** Read-only Bitbucket Cloud repo list using app password (user:token basic auth). */
public final class BitbucketClient {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(String result);
        void onError(String message);
    }

    /**
     * @param basicUserPass username:app_password
     */
    public void listRepos(String basicUserPass, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                if (basicUserPass == null || !basicUserPass.contains(":")) {
                    throw new IllegalArgumentException("Use username:app_password format.");
                }
                String auth = Base64.encodeToString(basicUserPass.trim().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                HttpURLConnection c = (HttpURLConnection) new URL(
                        "https://api.bitbucket.org/2.0/repositories?role=member&pagelen=20")
                        .openConnection();
                c.setRequestMethod("GET");
                c.setRequestProperty("Authorization", "Basic " + auth);
                c.setRequestProperty("User-Agent", "Kairo-Android/0.10");
                c.setConnectTimeout(15_000);
                c.setReadTimeout(30_000);
                int status = c.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
                String body = read(stream);
                c.disconnect();
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("Bitbucket HTTP " + status + ": " + body);
                }
                JSONObject root = new JSONObject(body);
                JSONArray values = root.optJSONArray("values");
                if (values == null || values.length() == 0) {
                    callback.onSuccess("No Bitbucket repositories found.");
                    return;
                }
                StringBuilder out = new StringBuilder("Bitbucket repositories\n");
                for (int i = 0; i < values.length(); i++) {
                    JSONObject r = values.optJSONObject(i);
                    if (r == null) continue;
                    out.append("• ").append(r.optString("full_name", r.optString("name"))).append('\n');
                }
                callback.onSuccess(out.toString().trim());
            } catch (Exception e) {
                callback.onError(e.getMessage() == null ? "Bitbucket request failed" : e.getMessage());
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
