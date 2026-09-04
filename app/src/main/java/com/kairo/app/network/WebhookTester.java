package com.kairo.app.network;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Explicit HTTPS webhook tester with an in-memory request log. */
public final class WebhookTester {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final List<String> LOG = Collections.synchronizedList(new ArrayList<>());

    public interface Callback {
        void onDone(String report);
    }

    private WebhookTester() {
    }

    public static List<String> logSnapshot() {
        synchronized (LOG) {
            return new ArrayList<>(LOG);
        }
    }

    public static void clearLog() {
        LOG.clear();
    }

    public static void send(String url, String jsonBody, Callback callback) {
        EXECUTOR.execute(() -> {
            long started = System.currentTimeMillis();
            String report;
            try {
                if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
                    throw new IllegalArgumentException("URL must start with https:// (http only for local tests).");
                }
                HttpURLConnection c = (HttpURLConnection) new URL(url.trim()).openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(15_000);
                c.setReadTimeout(30_000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setRequestProperty("User-Agent", "Kairo-Android-WebhookTester/1.0");
                byte[] payload = (jsonBody == null ? "{}" : jsonBody).getBytes(StandardCharsets.UTF_8);
                c.setFixedLengthStreamingMode(payload.length);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(payload);
                }
                int status = c.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
                String body = read(stream);
                long ms = System.currentTimeMillis() - started;
                report = "HTTP " + status + " in " + ms + "ms\n" + body;
                LOG.add(0, System.currentTimeMillis() + " · " + status + " · " + url + " · " + ms + "ms");
                while (LOG.size() > 30) LOG.remove(LOG.size() - 1);
                c.disconnect();
            } catch (Exception e) {
                report = "Error: " + (e.getMessage() == null ? "request failed" : e.getMessage());
                LOG.add(0, System.currentTimeMillis() + " · ERROR · " + url);
            }
            callback.onDone(report);
        });
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(line);
                if (sb.length() > 8000) {
                    sb.append("\n…");
                    break;
                }
            }
        }
        return sb.toString();
    }
}
