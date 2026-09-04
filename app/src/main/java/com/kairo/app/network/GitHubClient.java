package com.kairo.app.network;

import android.util.Base64;

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
 * Minimal GitHub REST tools for the GitHub Agent. Every write operation is explicit and is
 * invoked only by a user-confirmed UI action; this class never pushes in the background.
 */
public final class GitHubClient {
    private static final String API_ROOT = "https://api.github.com";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface ResultCallback {
        void onSuccess(String result);

        void onError(String message);
    }

    public void pullRepository(String token, String repository, ResultCallback callback) {
        run(() -> {
            JSONObject root = requestJson(token, "/repos/" + repo(repository), "GET", null);
            String fullName = root.optString("full_name", repository);
            String description = root.optString("description", "No description");
            String branch = root.optString("default_branch", "main");
            int stars = root.optInt("stargazers_count", 0);
            int openIssues = root.optInt("open_issues_count", 0);
            return fullName + "\n" + description + "\nDefault branch: " + branch
                    + "\nStars: " + stars + " · Open issues: " + openIssues;
        }, callback);
    }

    public void listIssues(String token, String repository, ResultCallback callback) {
        run(() -> {
            JSONArray issues = requestArray(token, "/repos/" + repo(repository)
                    + "/issues?state=open&per_page=20", "GET", null);
            if (issues.length() == 0) return "No open issues.";
            StringBuilder output = new StringBuilder();
            for (int index = 0; index < issues.length(); index++) {
                JSONObject issue = issues.optJSONObject(index);
                if (issue == null) continue;
                // Pull requests also appear in the issues endpoint. Mark them clearly for an agent.
                String kind = issue.has("pull_request") ? "PR" : "Issue";
                output.append("#").append(issue.optInt("number"))
                        .append(" [").append(kind).append("] ")
                        .append(issue.optString("title", "Untitled"))
                        .append("\n");
            }
            return output.toString().trim();
        }, callback);
    }

    public void readFile(String token, String repository, String path, String ref, ResultCallback callback) {
        run(() -> {
            String endpoint = "/repos/" + repo(repository) + "/contents/" + encodePath(path);
            if (ref != null && !ref.trim().isEmpty()) endpoint += "?ref=" + encode(ref.trim());
            JSONObject file = requestJson(token, endpoint, "GET", null);
            String encoded = file.optString("content", "").replace("\n", "");
            if (encoded.isEmpty()) return "The file was empty or GitHub returned no content.";
            byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
            return new String(bytes, StandardCharsets.UTF_8);
        }, callback);
    }

    /** Push or update one text file using GitHub's Contents API. */
    public void pushFile(
            String token,
            String repository,
            String path,
            String branch,
            String commitMessage,
            String content,
            ResultCallback callback) {
        run(() -> {
            String safeRepo = repo(repository);
            String safePath = encodePath(path);
            String safeBranch = branch == null || branch.trim().isEmpty() ? "main" : branch.trim();
            if (content != null && content.length() > 100_000) {
                throw new IllegalArgumentException("Kairo limits a single pushed text file to 100,000 characters.");
            }
            String endpoint = "/repos/" + safeRepo + "/contents/" + safePath;
            String sha = findExistingSha(token, endpoint, safeBranch);

            JSONObject body = new JSONObject();
            body.put("message", commitMessage == null || commitMessage.trim().isEmpty()
                    ? "Update from Kairo" : commitMessage.trim());
            body.put("content", Base64.encodeToString(
                    content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP));
            body.put("branch", safeBranch);
            if (sha != null) body.put("sha", sha);
            JSONObject result = requestJson(token, endpoint, "PUT", body);
            JSONObject commit = result.optJSONObject("commit");
            String html = result.optString("content", "");
            String url = commit == null ? "" : commit.optString("html_url", "");
            return (sha == null ? "Created " : "Updated ") + path
                    + " on " + safeBranch + (url.isEmpty() ? "" : "\n" + url);
        }, callback);
    }

    public void createPullRequest(
            String token,
            String repository,
            String title,
            String bodyText,
            String head,
            String base,
            ResultCallback callback) {
        run(() -> {
            JSONObject body = new JSONObject();
            body.put("title", title == null || title.trim().isEmpty() ? "Kairo change" : title.trim());
            body.put("body", bodyText == null ? "" : bodyText);
            body.put("head", head);
            body.put("base", base == null || base.trim().isEmpty() ? "main" : base.trim());
            JSONObject result = requestJson(token, "/repos/" + repo(repository) + "/pulls", "POST", body);
            return "Created PR #" + result.optInt("number") + "\n" + result.optString("html_url", "");
        }, callback);
    }

    private void run(Work work, ResultCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                callback.onSuccess(work.run());
            } catch (Throwable throwable) {
                String message = throwable.getMessage();
                callback.onError(message == null || message.trim().isEmpty()
                        ? "GitHub request failed." : message);
            }
        });
    }

    private interface Work {
        String run() throws Exception;
    }

    private JSONObject requestJson(String token, String path, String method, JSONObject body)
            throws Exception {
        String response = request(token, path, method, body);
        return new JSONObject(response);
    }

    private JSONArray requestArray(String token, String path, String method, JSONObject body)
            throws Exception {
        return new JSONArray(request(token, path, method, body));
    }

    private String request(String token, String path, String method, JSONObject body) throws Exception {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Add a GitHub token in Settings first.");
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_ROOT + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(60_000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            connection.setRequestProperty("Authorization", "Bearer " + token.trim());
            connection.setRequestProperty("User-Agent", "Kairo-Android/0.1");
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
                    detail = new JSONObject(response).optString("message", response);
                } catch (Exception ignored) {
                }
                throw new IllegalStateException("GitHub HTTP " + status + ": " + detail);
            }
            return response;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String findExistingSha(String token, String endpoint, String branch) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_ROOT + endpoint + "?ref=" + encode(branch)).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(60_000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("Authorization", "Bearer " + token.trim());
            int status = connection.getResponseCode();
            String response = read(status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream());
            if (status == 404) return null;
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("GitHub HTTP " + status);
            }
            return new JSONObject(response).optString("sha", null);
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

    private static String repo(String repository) {
        if (repository == null || !repository.trim().matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Use a repository in owner/name format.");
        }
        return repository.trim();
    }

    private static String encodePath(String path) throws Exception {
        if (path == null || path.trim().isEmpty()) throw new IllegalArgumentException("File path is required.");
        String[] pieces = path.trim().split("/");
        StringBuilder result = new StringBuilder();
        for (String piece : pieces) {
            if (piece.isEmpty() || ".".equals(piece) || "..".equals(piece)) {
                throw new IllegalArgumentException("Invalid file path.");
            }
            if (result.length() > 0) result.append('/');
            result.append(encode(piece));
        }
        return result.toString();
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
    }
}
