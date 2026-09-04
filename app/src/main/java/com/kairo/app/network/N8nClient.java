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

/**
 * n8n REST and webhook adapter. Workflow activation and webhook execution are deliberately
 * separate calls so the UI can request confirmation before anything runs or changes state.
 */
public final class N8nClient {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(String result);
        void onError(String message);
    }

    public void listWorkflows(String token, String baseUrl, Callback callback) {
        run(() -> {
            JSONObject root = requestJson(token, baseUrl, "/workflows?limit=20", "GET", null);
            JSONArray workflows = root.optJSONArray("data");
            if (workflows == null || workflows.length() == 0) return "No n8n workflows found.";
            StringBuilder output = new StringBuilder("n8n workflows\n");
            for (int index = 0; index < workflows.length(); index++) {
                JSONObject workflow = workflows.optJSONObject(index);
                if (workflow == null) continue;
                output.append("• ").append(workflow.optString("name", "Unnamed workflow"))
                        .append("  ·  ").append(workflow.optString("id", "no id"))
                        .append("  ·  ").append(workflow.optBoolean("active", false) ? "ACTIVE" : "PAUSED")
                        .append('\n');
            }
            return output.toString().trim();
        }, callback);
    }

    public void listExecutions(String token, String baseUrl, Callback callback) {
        run(() -> {
            JSONObject root = requestJson(token, baseUrl, "/executions?limit=20", "GET", null);
            JSONArray executions = root.optJSONArray("data");
            if (executions == null || executions.length() == 0) return "No recent n8n executions found.";
            StringBuilder output = new StringBuilder("Recent executions\n");
            for (int index = 0; index < executions.length(); index++) {
                JSONObject execution = executions.optJSONObject(index);
                if (execution == null) continue;
                output.append("• #").append(execution.optString("id", "?"))
                        .append("  ·  ").append(execution.optString("status", "unknown"))
                        .append("  ·  ").append(execution.optString("workflowName", "workflow unavailable"))
                        .append('\n');
            }
            return output.toString().trim();
        }, callback);
    }

    /** Activate one workflow through n8n's explicit activation endpoint. */
    public void activateWorkflow(String token, String baseUrl, String workflowId, Callback callback) {
        run(() -> {
            String id = requireId(workflowId);
            request(token, baseUrl, "/workflows/" + encodePathPart(id) + "/activate", "POST", null);
            return "Workflow " + id + " activated.";
        }, callback);
    }

    /**
     * Call a user-configured n8n webhook with a JSON payload. The URL is not inferred from
     * workflow names, which prevents an agent from silently executing an unintended workflow.
     */
    public void runWebhook(String webhookUrl, String payload, Callback callback) {
        run(() -> {
            if (webhookUrl == null || !(webhookUrl.trim().startsWith("https://")
                    || webhookUrl.trim().startsWith("http://"))) {
                throw new IllegalArgumentException("Configure an http(s) n8n webhook URL first.");
            }
            String cleanPayload = payload == null || payload.trim().isEmpty() ? "{}" : payload.trim();
            JSONObject parsed = new JSONObject(cleanPayload);
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(webhookUrl.trim()).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15_000);
                connection.setReadTimeout(60_000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                // Webhooks authenticate independently (usually with a secret path or header).
                // Never forward the n8n management API key to an arbitrary webhook URL.
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(parsed.toString().getBytes(StandardCharsets.UTF_8));
                }
                int status = connection.getResponseCode();
                String response = read(status >= 200 && status < 300
                        ? connection.getInputStream() : connection.getErrorStream());
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("n8n webhook HTTP " + status + ": " + response);
                }
                return "Webhook accepted\n" + (response.isEmpty() ? "(empty response)" : response);
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
                        ? "n8n request failed." : message);
            }
        });
    }

    private interface Work {
        String run() throws Exception;
    }

    private JSONObject requestJson(String token, String baseUrl, String path, String method, JSONObject body)
            throws Exception {
        String response = request(token, baseUrl, path, method, body);
        return new JSONObject(response);
    }

    private String request(String token, String baseUrl, String path, String method, JSONObject body)
            throws Exception {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Add an n8n API key in Settings first.");
        }
        String apiBase = normalizeApiBase(baseUrl);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(apiBase + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(60_000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-N8N-API-KEY", token.trim());
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
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("n8n HTTP " + status + ": " + response);
            }
            return response;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String normalizeApiBase(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Configure the n8n base URL in Settings first.");
        }
        String value = baseUrl.trim();
        if (!(value.startsWith("https://") || value.startsWith("http://"))) {
            throw new IllegalArgumentException("n8n base URL must start with http:// or https://.");
        }
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (!value.endsWith("/api/v1")) value += "/api/v1";
        return value;
    }

    private static String requireId(String value) {
        if (value == null || !value.trim().matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Use a valid n8n workflow id.");
        }
        return value.trim();
    }

    private static String encodePathPart(String value) {
        return value.replace("%", "%25").replace("/", "%2F");
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
