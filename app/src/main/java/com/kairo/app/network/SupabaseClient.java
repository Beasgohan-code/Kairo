package com.kairo.app.network;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bounded Supabase REST connector. It intentionally exposes a read-only table preview rather
 * than embedding a service-role SDK or giving an agent unrestricted database access.
 */
public final class SupabaseClient {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(String result);
        void onError(String message);
    }

    public void readTable(String baseUrl, String apiKey, String table, Callback callback) {
        run(() -> {
            String safeTable = requireTable(table);
            String base = normalizeBase(baseUrl);
            String endpoint = base + "/rest/v1/" + safeTable + "?select=*&limit=20";
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15_000);
                connection.setReadTimeout(45_000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("apikey", requireKey(apiKey));
                connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                int status = connection.getResponseCode();
                String response = read(status >= 200 && status < 300
                        ? connection.getInputStream() : connection.getErrorStream());
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("Supabase HTTP " + status + ": " + response);
                }
                if (response.length() > 30_000) response = response.substring(0, 30_000) + "\n… preview truncated";
                return "Supabase table: " + safeTable + "\n" + response;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, callback);
    }

    private void run(Work work, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                callback.onSuccess(work.run());
            } catch (Throwable throwable) {
                String message = throwable.getMessage();
                callback.onError(message == null || message.trim().isEmpty()
                        ? "Supabase request failed." : message);
            }
        });
    }

    private interface Work {
        String run() throws Exception;
    }

    private static String normalizeBase(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Configure the Supabase project URL first.");
        }
        String base = baseUrl.trim();
        if (!(base.startsWith("https://") || base.startsWith("http://"))) {
            throw new IllegalArgumentException("Supabase URL must start with http:// or https://.");
        }
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }

    private static String requireKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Add a Supabase anon or scoped key first.");
        }
        return apiKey.trim();
    }

    private static String requireTable(String table) {
        if (table == null || !table.trim().matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Use a simple Supabase table name, for example notes.");
        }
        return table.trim();
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
