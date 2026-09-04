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

/** Read-only Linear connector for issue discovery and handoff planning. */
public final class LinearClient {
    private static final String API_ROOT = "https://api.linear.app/graphql";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(String result);
        void onError(String message);
    }

    public void test(String token, Callback callback) {
        run(() -> {
            JSONObject root = request(token,
                    "query { viewer { id name } }", new JSONObject());
            JSONObject viewer = root.optJSONObject("data");
            viewer = viewer == null ? null : viewer.optJSONObject("viewer");
            if (viewer == null) throw new IllegalStateException("Linear returned no viewer.");
            return "Linear connected as " + viewer.optString("name", "workspace member")
                    + " (" + viewer.optString("id", "id unavailable") + ").";
        }, callback);
    }

    public void searchIssues(String token, String query, Callback callback) {
        run(() -> {
            String term = query == null ? "" : query.trim();
            if (term.length() > 200) throw new IllegalArgumentException("Linear searches are limited to 200 characters.");
            JSONObject variables = new JSONObject();
            variables.put("term", term);
            JSONObject root = request(token,
                    "query SearchIssues($term: String!) { issueSearch(query: $term, first: 20) { nodes { identifier title url state { name } team { name } } } }",
                    variables);
            JSONObject data = root.optJSONObject("data");
            JSONObject connection = data == null ? null : data.optJSONObject("issueSearch");
            JSONArray nodes = connection == null ? null : connection.optJSONArray("nodes");
            if (nodes == null || nodes.length() == 0) return "No matching Linear issues found.";
            StringBuilder output = new StringBuilder("Linear issues\n");
            for (int index = 0; index < nodes.length(); index++) {
                JSONObject issue = nodes.optJSONObject(index);
                if (issue == null) continue;
                JSONObject state = issue.optJSONObject("state");
                JSONObject team = issue.optJSONObject("team");
                output.append("• ").append(issue.optString("identifier", "Issue"))
                        .append(" · ").append(issue.optString("title", "Untitled"))
                        .append("\n  ").append(team == null ? "" : team.optString("name", ""))
                        .append(" · ").append(state == null ? "" : state.optString("name", ""))
                        .append("\n  ").append(issue.optString("url", "No issue URL"))
                        .append('\n');
            }
            return output.toString().trim();
        }, callback);
    }

    private void run(Work work, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                callback.onSuccess(work.run());
            } catch (Throwable throwable) {
                String message = throwable.getMessage();
                callback.onError(message == null || message.trim().isEmpty()
                        ? "Linear request failed." : message);
            }
        });
    }

    private interface Work {
        String run() throws Exception;
    }

    private JSONObject request(String token, String query, JSONObject variables) throws Exception {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Add a Linear API key in Settings first.");
        }
        JSONObject body = new JSONObject();
        body.put("query", query);
        body.put("variables", variables == null ? new JSONObject() : variables);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_ROOT).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(45_000);
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", token.trim());
            connection.setRequestProperty("User-Agent", "Kairo-Android/0.3");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = read(stream);
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Linear HTTP " + status + ": " + response);
            }
            JSONObject root = new JSONObject(response);
            JSONArray errors = root.optJSONArray("errors");
            if (errors != null && errors.length() > 0) {
                JSONObject error = errors.optJSONObject(0);
                throw new IllegalStateException(error == null
                        ? "Linear returned a GraphQL error." : error.optString("message", "Linear returned a GraphQL error."));
            }
            return root;
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
