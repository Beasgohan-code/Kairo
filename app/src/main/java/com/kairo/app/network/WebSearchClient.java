package com.kairo.app.network;

import com.kairo.app.core.NetworkErrors;
import com.kairo.app.data.SearchResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Direct web search with an optional Brave API key and a no-key DuckDuckGo fallback.
 * Results are shown before insert into a prompt.
 */
public final class WebSearchClient {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(List<SearchResult> results, String provider);

        void onError(String message);
    }

    public void search(String query, String braveKey, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                String clean = query == null ? "" : query.trim();
                if (clean.isEmpty()) {
                    throw new IllegalArgumentException("Enter a search query.");
                }
                if (braveKey != null && !braveKey.trim().isEmpty()) {
                    callback.onSuccess(searchBrave(clean, braveKey.trim()), "Brave Search");
                } else {
                    callback.onSuccess(searchDuckDuckGo(clean), "DuckDuckGo Instant Answers");
                }
            } catch (Throwable throwable) {
                callback.onError(NetworkErrors.friendly(throwable));
            }
        });
    }

    private List<SearchResult> searchBrave(String query, String key) throws Exception {
        String endpoint = "https://api.search.brave.com/res/v1/web/search?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8.name()) + "&count=8";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-Subscription-Token", key);
            connection.setRequestProperty("User-Agent", "Kairo-Android/0.11");
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String body = read(stream);
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Brave Search HTTP " + status + ": " + body);
            }
            JSONObject root = new JSONObject(body);
            JSONObject web = root.optJSONObject("web");
            JSONArray results = web == null ? null : web.optJSONArray("results");
            List<SearchResult> output = new ArrayList<>();
            if (results != null) {
                for (int i = 0; i < results.length(); i++) {
                    JSONObject item = results.optJSONObject(i);
                    if (item == null) continue;
                    output.add(new SearchResult(
                            item.optString("title", "Untitled"),
                            item.optString("url", ""),
                            item.optString("description", item.optString("snippet", "")),
                            "Brave"));
                }
            }
            if (output.isEmpty()) {
                output.add(new SearchResult("No Brave results", "", "Try another query.", "Brave"));
            }
            return output;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private List<SearchResult> searchDuckDuckGo(String query) throws Exception {
        String endpoint = "https://api.duckduckgo.com/?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                + "&format=json&no_html=1&skip_disambig=1";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Kairo-Android/0.11");
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String body = read(stream);
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("DuckDuckGo HTTP " + status);
            }
            JSONObject root = new JSONObject(body);
            List<SearchResult> output = new ArrayList<>();
            String heading = root.optString("Heading", "");
            String abstractText = root.optString("AbstractText", "");
            String abstractUrl = root.optString("AbstractURL", "");
            if (!abstractText.isEmpty()) {
                output.add(new SearchResult(
                        heading.isEmpty() ? query : heading,
                        abstractUrl,
                        abstractText,
                        "DuckDuckGo"));
            }
            addTopics(root.optJSONArray("RelatedTopics"), output, 8);
            if (output.isEmpty()) {
                output.add(new SearchResult(
                        "No instant answer",
                        "https://duckduckgo.com/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8.name()),
                        "Open DuckDuckGo for full results.",
                        "DuckDuckGo"));
            }
            return output;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void addTopics(JSONArray topics, List<SearchResult> output, int limit) {
        if (topics == null) return;
        for (int index = 0; index < topics.length() && output.size() < limit; index++) {
            JSONObject item = topics.optJSONObject(index);
            if (item == null) continue;
            String firstUrl = item.optString("FirstURL", "");
            String text = item.optString("Text", "");
            if (!firstUrl.isEmpty() && !text.isEmpty()) {
                output.add(new SearchResult(text, firstUrl, text, "DuckDuckGo"));
            } else {
                addTopics(item.optJSONArray("Topics"), output, limit);
            }
        }
    }

    private String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }
}
