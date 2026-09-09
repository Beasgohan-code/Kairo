package com.kairo.app.network;

import com.kairo.app.core.NetworkErrors;
import com.kairo.app.core.ProviderConfig;
import com.kairo.app.data.ChatAttachment;
import com.kairo.app.data.ChatMessage;
import com.kairo.app.data.ModelInfo;

import org.json.JSONArray;
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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Small dependency-free HTTP client for OpenAI-compatible, Anthropic, and Ollama APIs. */
public final class ApiClient {
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 120_000;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private ApiClient() {
    }

    public interface ChatCallback {
        void onSuccess(String answer);

        void onError(String message);
    }

    public interface ModelsCallback {
        void onSuccess(List<ModelInfo> models);

        void onError(String message);
    }

    /** Token callback used by the chat UI for a responsive, Claude-like streaming answer. */
    public interface StreamingCallback {
        void onToken(String token);

        void onComplete();

        void onError(String message);
    }

    /** Allows the user to stop an in-flight response without waiting for the provider timeout. */
    public static final class RequestHandle {
        private volatile boolean cancelled;
        private Future<?> future;

        private void attach(Future<?> value) {
            future = value;
        }

        public void cancel() {
            cancelled = true;
            if (future != null) future.cancel(true);
        }

        public boolean isCancelled() {
            return cancelled || Thread.currentThread().isInterrupted();
        }
    }

    public static void sendChat(
            String providerId,
            String baseUrl,
            String apiKey,
            String modelId,
            List<ChatMessage> messages,
            ChatCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                String answer;
                if (ProviderConfig.usesAnthropicApi(providerId)) {
                    answer = sendAnthropic(baseUrl, apiKey, modelId, messages);
                } else {
                    answer = sendOpenAiCompatible(baseUrl, apiKey, modelId, messages);
                }
                callback.onSuccess(answer);
            } catch (Throwable throwable) {
                callback.onError(NetworkErrors.friendly(throwable));
            }
        });
    }

    /**
     * Stream text deltas from OpenAI-compatible providers or Anthropic. Most modern Groq,
     * NVIDIA, OpenRouter, OpenAI, and Ollama-compatible endpoints support this SSE shape.
     */
    public static RequestHandle sendChatStreaming(
            String providerId,
            String baseUrl,
            String apiKey,
            String modelId,
            List<ChatMessage> messages,
            StreamingCallback callback) {
        return sendChatStreaming(providerId, baseUrl, apiKey, modelId, messages,
                Collections.emptyList(), callback);
    }

    /** Stream a conversation and attach bounded images to the latest user message. */
    public static RequestHandle sendChatStreaming(
            String providerId,
            String baseUrl,
            String apiKey,
            String modelId,
            List<ChatMessage> messages,
            List<ChatAttachment> attachments,
            StreamingCallback callback) {
        return sendChatStreaming(providerId, baseUrl, apiKey, modelId, messages, attachments,
                0.2f, 2048, "balanced", callback);
    }

    /** Stream with user-tunable generation controls shared by Claude/Groq-style providers. */
    public static RequestHandle sendChatStreaming(
            String providerId,
            String baseUrl,
            String apiKey,
            String modelId,
            List<ChatMessage> messages,
            List<ChatAttachment> attachments,
            float temperature,
            int maxOutputTokens,
            StreamingCallback callback) {
        return sendChatStreaming(providerId, baseUrl, apiKey, modelId, messages, attachments,
                temperature, maxOutputTokens, "balanced", callback);
    }

    /** Stream with generation controls and optional native Kimi reasoning effort. */
    public static RequestHandle sendChatStreaming(
            String providerId,
            String baseUrl,
            String apiKey,
            String modelId,
            List<ChatMessage> messages,
            List<ChatAttachment> attachments,
            float temperature,
            int maxOutputTokens,
            String reasoningMode,
            StreamingCallback callback) {
        final float safeTemperature = Math.max(0f, Math.min(2f, temperature));
        final String safeReasoningMode = "fast".equals(reasoningMode) || "deep".equals(reasoningMode)
                ? reasoningMode : "balanced";
        final int safeMaxOutputTokens = Math.max(256, Math.min(8192, maxOutputTokens));
        RequestHandle handle = new RequestHandle();
        Future<?> future = EXECUTOR.submit(() -> {
            try {
                if (ProviderConfig.usesAnthropicApi(providerId)) {
                    streamAnthropic(baseUrl, apiKey, modelId, messages, attachments,
                            safeTemperature, safeMaxOutputTokens, callback, handle);
                } else {
                    streamOpenAiCompatible(providerId, baseUrl, apiKey, modelId, messages, attachments,
                            safeTemperature, safeMaxOutputTokens, safeReasoningMode, callback, handle);
                }
                if (!handle.isCancelled()) callback.onComplete();
            } catch (Throwable throwable) {
                if (!handle.isCancelled()) callback.onError(NetworkErrors.friendly(throwable));
            }
        });
        handle.attach(future);
        return handle;
    }

    public static void discoverModels(
            String providerId,
            String baseUrl,
            String apiKey,
            ModelsCallback callback) {
        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                if (ProviderConfig.usesAnthropicApi(providerId)) {
                    throw new IllegalArgumentException(
                            "Anthropic does not expose a public model list here. Choose a model in the catalog.");
                }
                connection = openConnection(withVersionPath(baseUrl, "/models"), "GET", apiKey);
                String body = readResponse(connection);
                JSONObject root = new JSONObject(body);
                JSONArray data = root.optJSONArray("data");
                if (data == null) {
                    throw new IllegalStateException("The provider returned no model list.");
                }
                List<ModelInfo> models = new ArrayList<>();
                for (int index = 0; index < data.length(); index++) {
                    JSONObject item = data.optJSONObject(index);
                    if (item == null) continue;
                    String id = item.optString("id", "").trim();
                    if (id.isEmpty()) continue;
                    boolean free = "openrouter".equals(providerId)
                            && id.toLowerCase(Locale.US).endsWith(":free");
                    boolean local = "ollama".equals(providerId);
                    models.add(new ModelInfo(
                            id,
                            prettyName(id),
                            providerId,
                            "Discovered from the provider's live model list",
                            free,
                            local,
                            "Provider supplied",
                            free ? "Free route; limits vary" : "Provider billing / limits apply"));
                    if (models.size() >= 250) break;
                }
                callback.onSuccess(models);
            } catch (Throwable throwable) {
                callback.onError(NetworkErrors.friendly(throwable));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private static String sendOpenAiCompatible(
            String baseUrl,
            String apiKey,
            String modelId,
            List<ChatMessage> messages) throws Exception {
        JSONObject request = new JSONObject();
        request.put("model", modelId);
        request.put("messages", messageArray(messages));
        request.put("stream", false);
        request.put("temperature", 0.2);

        HttpURLConnection connection = null;
        try {
            connection = openConnection(withVersionPath(baseUrl, "/chat/completions"), "POST", apiKey);
            writeJson(connection, request.toString());
            String body = readResponse(connection);
            JSONObject root = new JSONObject(body);
            JSONArray choices = root.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                throw providerError(root, "The provider returned no answer.");
            }
            JSONObject first = choices.optJSONObject(0);
            if (first == null) throw new IllegalStateException("The provider returned an invalid answer.");
            JSONObject message = first.optJSONObject("message");
            if (message == null) throw new IllegalStateException("The provider returned no message.");
            String answer = contentAsString(message.opt("content"));
            if (answer.trim().isEmpty()) throw new IllegalStateException("The provider returned an empty answer.");
            return answer.trim();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String sendAnthropic(
            String baseUrl,
            String apiKey,
            String modelId,
            List<ChatMessage> messages) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Add an Anthropic API key in Settings first.");
        }
        JSONObject request = new JSONObject();
        request.put("model", modelId);
        request.put("max_tokens", 2048);
        JSONArray apiMessages = new JSONArray();
        StringBuilder systemPrompt = new StringBuilder();
        for (ChatMessage message : messages) {
            if ("system".equals(message.getRole())) {
                if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
                systemPrompt.append(message.getContent());
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("role", "assistant".equals(message.getRole()) ? "assistant" : "user");
            item.put("content", message.getContent());
            apiMessages.put(item);
        }
        request.put("messages", apiMessages);
        if (systemPrompt.length() > 0) request.put("system", systemPrompt.toString());

        HttpURLConnection connection = null;
        try {
            connection = openConnection(withVersionPath(baseUrl, "/v1/messages"), "POST", null);
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setRequestProperty("anthropic-version", "2023-06-01");
            writeJson(connection, request.toString());
            JSONObject root = new JSONObject(readResponse(connection));
            JSONArray content = root.optJSONArray("content");
            if (content == null || content.length() == 0) {
                throw providerError(root, "Anthropic returned no answer.");
            }
            String answer = contentAsString(content.optJSONObject(0));
            if (answer.trim().isEmpty()) throw new IllegalStateException("Anthropic returned an empty answer.");
            return answer.trim();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void streamOpenAiCompatible(
            String providerId,
            String baseUrl,
            String apiKey,
            String modelId,
            List<ChatMessage> messages,
            List<ChatAttachment> attachments,
            float temperature,
            int maxOutputTokens,
            String reasoningMode,
            StreamingCallback callback,
            RequestHandle handle) throws Exception {
        JSONObject request = new JSONObject();
        request.put("model", modelId);
        request.put("messages", messageArray(messages, attachments));
        request.put("stream", true);
        request.put("temperature", temperature);
        request.put("max_tokens", maxOutputTokens);
        if ("moonshot".equals(providerId) && "kimi-k3".equalsIgnoreCase(modelId)) {
            // Kimi K3 exposes native reasoning_effort in its OpenAI-compatible API.
            request.put("reasoning_effort", "deep".equals(reasoningMode) ? "max"
                    : ("fast".equals(reasoningMode) ? "low" : "high"));
        }

        HttpURLConnection connection = null;
        try {
            connection = openConnection(withVersionPath(baseUrl, "/chat/completions"), "POST", apiKey);
            writeJson(connection, request.toString());
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            if (status < 200 || status >= 300) {
                String body = readStream(stream);
                try {
                    throw providerError(new JSONObject(body), "Request failed with HTTP " + status);
                } catch (org.json.JSONException ignored) {
                    throw new IllegalStateException("HTTP " + status + ": " + body);
                }
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder nonSseResponse = new StringBuilder();
                boolean emittedToken = false;
                String line;
                while (!handle.isCancelled() && (line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        if (!line.trim().isEmpty()) nonSseResponse.append(line.trim());
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    if (data.isEmpty()) continue;
                    JSONObject chunk = new JSONObject(data);
                    JSONArray choices = chunk.optJSONArray("choices");
                    if (choices == null || choices.length() == 0) continue;
                    JSONObject choice = choices.optJSONObject(0);
                    if (choice == null) continue;
                    JSONObject delta = choice.optJSONObject("delta");
                    if (delta == null) continue;
                    String token = contentAsString(delta.opt("content"));
                    if (!token.isEmpty()) {
                        emittedToken = true;
                        callback.onToken(token);
                    }
                }
                // A few compatible servers ignore stream=true and return one regular JSON body.
                if (!emittedToken && !nonSseResponse.toString().isEmpty() && !handle.isCancelled()) {
                    JSONObject complete = new JSONObject(nonSseResponse.toString());
                    JSONArray choices = complete.optJSONArray("choices");
                    if (choices != null && choices.length() > 0) {
                        JSONObject choice = choices.optJSONObject(0);
                        JSONObject message = choice == null ? null : choice.optJSONObject("message");
                        String token = message == null ? "" : contentAsString(message.opt("content"));
                        if (!token.isEmpty()) callback.onToken(token);
                    }
                }
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void streamAnthropic(
            String baseUrl,
            String apiKey,
            String modelId,
            List<ChatMessage> messages,
            List<ChatAttachment> attachments,
            float temperature,
            int maxOutputTokens,
            StreamingCallback callback,
            RequestHandle handle) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Add an Anthropic API key in Settings first.");
        }
        JSONObject request = new JSONObject();
        request.put("model", modelId);
        request.put("max_tokens", maxOutputTokens);
        request.put("temperature", Math.min(1f, temperature));
        request.put("stream", true);
        JSONArray apiMessages = new JSONArray();
        StringBuilder systemPrompt = new StringBuilder();
        for (int index = 0; index < messages.size(); index++) {
            ChatMessage message = messages.get(index);
            if ("system".equals(message.getRole())) {
                if (systemPrompt.length() > 0) systemPrompt.append("\n\n");
                systemPrompt.append(message.getContent());
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("role", "assistant".equals(message.getRole()) ? "assistant" : "user");
            boolean latestUser = index == messages.size() - 1 && "user".equals(message.getRole());
            item.put("content", anthropicContent(message.getContent(), latestUser ? attachments : Collections.emptyList()));
            apiMessages.put(item);
        }
        request.put("messages", apiMessages);
        if (systemPrompt.length() > 0) request.put("system", systemPrompt.toString());

        HttpURLConnection connection = null;
        try {
            connection = openConnection(withVersionPath(baseUrl, "/v1/messages"), "POST", null);
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setRequestProperty("anthropic-version", "2023-06-01");
            writeJson(connection, request.toString());
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            if (status < 200 || status >= 300) {
                String body = readStream(stream);
                try {
                    throw providerError(new JSONObject(body), "Request failed with HTTP " + status);
                } catch (org.json.JSONException ignored) {
                    throw new IllegalStateException("HTTP " + status + ": " + body);
                }
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String event = "";
                String line;
                while (!handle.isCancelled() && (line = reader.readLine()) != null) {
                    if (line.startsWith("event:")) {
                        event = line.substring(6).trim();
                        continue;
                    }
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) continue;
                    JSONObject chunk = new JSONObject(data);
                    if ("content_block_delta".equals(event)
                            || "content_block_delta".equals(chunk.optString("type"))) {
                        JSONObject delta = chunk.optJSONObject("delta");
                        if (delta != null) {
                            String token = delta.optString("text", "");
                            if (!token.isEmpty()) callback.onToken(token);
                        }
                    }
                    if ("message_stop".equals(event)
                            || "message_stop".equals(chunk.optString("type"))) break;
                }
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static JSONArray messageArray(List<ChatMessage> messages) throws Exception {
        return messageArray(messages, Collections.emptyList());
    }

    private static JSONArray messageArray(
            List<ChatMessage> messages,
            List<ChatAttachment> attachments) throws Exception {
        JSONArray result = new JSONArray();
        for (int index = 0; index < messages.size(); index++) {
            ChatMessage message = messages.get(index);
            JSONObject item = new JSONObject();
            item.put("role", message.getRole());
            boolean latestUser = index == messages.size() - 1 && "user".equals(message.getRole());
            if (latestUser && attachments != null && !attachments.isEmpty()) {
                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("type", "text");
                textPart.put("text", message.getContent());
                parts.put(textPart);
                for (ChatAttachment attachment : attachments) {
                    JSONObject imagePart = new JSONObject();
                    imagePart.put("type", "image_url");
                    JSONObject imageUrl = new JSONObject();
                    imageUrl.put("url", "data:" + attachment.getMimeType()
                            + ";base64," + attachment.getBase64());
                    imageUrl.put("detail", "auto");
                    imagePart.put("image_url", imageUrl);
                    parts.put(imagePart);
                }
                item.put("content", parts);
            } else {
                item.put("content", message.getContent());
            }
            result.put(item);
        }
        return result;
    }

    private static Object anthropicContent(String text, List<ChatAttachment> attachments)
            throws Exception {
        if (attachments == null || attachments.isEmpty()) return text;
        JSONArray content = new JSONArray();
        JSONObject textBlock = new JSONObject();
        textBlock.put("type", "text");
        textBlock.put("text", text);
        content.put(textBlock);
        for (ChatAttachment attachment : attachments) {
            JSONObject imageBlock = new JSONObject();
            imageBlock.put("type", "image");
            JSONObject source = new JSONObject();
            source.put("type", "base64");
            source.put("media_type", attachment.getMimeType());
            source.put("data", attachment.getBase64());
            imageBlock.put("source", source);
            content.put(imageBlock);
        }
        return content;
    }

    private static HttpURLConnection openConnection(String endpoint, String method, String apiKey)
            throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Kairo-Android/0.1");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        }
        if ("POST".equals(method)) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        }
        return connection;
    }

    private static void writeJson(HttpURLConnection connection, String json) throws Exception {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }
    }

    private static String readResponse(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String body = readStream(stream);
        if (status < 200 || status >= 300) {
            String detail = body;
            try {
                detail = providerError(new JSONObject(body), "Request failed with HTTP " + status).getMessage();
            } catch (Exception ignored) {
                // Keep the plain response when the provider did not return JSON.
            }
            throw new IllegalStateException("HTTP " + status + ": " + detail);
        }
        return body;
    }

    private static String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static IllegalStateException providerError(JSONObject root, String fallback) {
        String message = root.optString("message", "");
        JSONObject error = root.optJSONObject("error");
        if (message.isEmpty() && error != null) message = error.optString("message", "");
        return new IllegalStateException(message.isEmpty() ? fallback : message);
    }

    private static String contentAsString(Object content) {
        if (content == null || content == JSONObject.NULL) return "";
        if (content instanceof String) return (String) content;
        if (content instanceof JSONObject) {
            return ((JSONObject) content).optString("text", content.toString());
        }
        if (content instanceof JSONArray) {
            StringBuilder result = new StringBuilder();
            JSONArray parts = (JSONArray) content;
            for (int index = 0; index < parts.length(); index++) {
                Object part = parts.opt(index);
                if (part instanceof JSONObject) {
                    result.append(((JSONObject) part).optString("text", ""));
                } else if (part != null) {
                    result.append(part);
                }
            }
            return result.toString();
        }
        return String.valueOf(content);
    }

    private static String withVersionPath(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.endsWith("/v1") && path.startsWith("/v1/")) {
            path = path.substring(3);
        }
        if ("/chat/completions".equals(path)
                && !base.endsWith("/v1")
                && (base.contains("11434") || base.contains("localhost") || base.contains("127.0.0.1"))) {
            base += "/v1";
        }
        return base + path;
    }

    private static String prettyName(String id) {
        String value = id;
        int slash = value.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < value.length()) value = value.substring(slash + 1);
        return value.replace('-', ' ').replace('_', ' ');
    }
}
