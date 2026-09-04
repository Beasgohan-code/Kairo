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

/**
 * Small Vercel REST adapter. Read operations are safe to run from the Connectors screen;
 * deployment creation is only called by an explicit confirmation action in the UI.
 */
public final class VercelClient {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(String result);

        void onError(String message);
    }

    public void listProjects(String token, String baseUrl, String teamId, Callback callback) {
        run(() -> {
            String path = "/v9/projects?limit=20";
            if (teamId != null && !teamId.trim().isEmpty()) path += "&teamId=" + encode(teamId.trim());
            JSONObject root = requestJson(token, baseUrl, path, "GET", null);
            JSONArray projects = root.optJSONArray("projects");
            if (projects == null || projects.length() == 0) return "No Vercel projects found.";
            StringBuilder output = new StringBuilder("Vercel projects\n");
            for (int index = 0; index < projects.length(); index++) {
                JSONObject project = projects.optJSONObject(index);
                if (project == null) continue;
                output.append("• ").append(project.optString("name", "Unnamed project"))
                        .append("  ·  ").append(project.optString("id", "no project id"));
                String framework = project.optString("framework", "");
                if (!framework.isEmpty()) output.append("  ·  ").append(framework);
                output.append('\n');
            }
            return output.toString().trim();
        }, callback);
    }

    public void listDeployments(
            String token,
            String baseUrl,
            String teamId,
            String project,
            Callback callback) {
        run(() -> {
            String path = "/v6/deployments?limit=20";
            if (teamId != null && !teamId.trim().isEmpty()) path += "&teamId=" + encode(teamId.trim());
            if (project != null && !project.trim().isEmpty()) {
                path += "&projectId=" + encode(project.trim());
            }
            JSONObject root = requestJson(token, baseUrl, path, "GET", null);
            JSONArray deployments = root.optJSONArray("deployments");
            if (deployments == null || deployments.length() == 0) return "No deployments found.";
            StringBuilder output = new StringBuilder("Recent deployments\n");
            for (int index = 0; index < deployments.length(); index++) {
                JSONObject d = deployments.optJSONObject(index);
                if (d == null) continue;
                output.append("• ").append(d.optString("name", d.optString("id", "deployment")))
                        .append("  ·  ").append(d.optString("state", d.optString("readyState", "?")));
                String url = d.optString("url", "");
                if (!url.isEmpty()) output.append("  ·  https://").append(url);
                output.append('\n');
            }
            return output.toString().trim();
        }, callback);
    }

    public void createDeployment(
            String token,
            String baseUrl,
            String teamId,
            String project,
            String repository,
            String ref,
            Callback callback) {
        run(() -> {
            if (project == null || project.trim().isEmpty()) {
                throw new IllegalArgumentException("Add a Vercel project name or id first.");
            }
            if (repository == null || !repository.trim().matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
                throw new IllegalArgumentException("Use a Git repository in owner/name format.");
            }
            JSONObject gitSource = new JSONObject();
            gitSource.put("type", "github");
            gitSource.put("repo", repository.trim());
            gitSource.put("ref", ref == null || ref.trim().isEmpty() ? "main" : ref.trim());

            JSONObject body = new JSONObject();
            body.put("name", project.trim());
            body.put("project", project.trim());
            body.put("target", "production");
            body.put("gitSource", gitSource);
            String path = "/v13/deployments";
            if (teamId != null && !teamId.trim().isEmpty()) path += "?teamId=" + encode(teamId.trim());
            JSONObject result = requestJson(token, baseUrl, path, "POST", body);
            String url = result.optString("url", "");
            String id = result.optString("id", "unknown");
            return "Deployment created\nID: " + id + (url.isEmpty() ? "" : "\nhttps://" + url);
        }, callback);
    }

    private void run(Work work, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                callback.onSuccess(work.run());
            } catch (Throwable throwable) {
                String message = throwable.getMessage();
                callback.onError(message == null || message.trim().isEmpty()
                        ? "Vercel request failed." : message);
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
            throw new IllegalArgumentException("Add a Vercel token in Settings first.");
        }
        String base = baseUrl == null || baseUrl.trim().isEmpty() ? "https://api.vercel.com" : baseUrl.trim();
        if (!(base.startsWith("https://") || base.startsWith("http://"))) {
            throw new IllegalArgumentException("Vercel API URL must start with http:// or https://.");
        }
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(base + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(60_000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token.trim());
            connection.setRequestProperty("User-Agent", "Kairo-Android/0.8");
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
                String detail = response;
                try {
                    detail = new JSONObject(response).optString("error", response);
                    if (detail.startsWith("{")) {
                        detail = new JSONObject(detail).optString("message", detail);
                    }
                } catch (Exception ignored) {
                }
                throw new IllegalStateException("Vercel HTTP " + status + ": " + detail);
            }
            return response;
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

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
    }
}
