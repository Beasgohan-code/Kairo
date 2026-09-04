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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OpenAI-compatible image generation (POST /images/generations).
 * Works with OpenAI and compatible gateways. Always invoked from an explicit UI action.
 */
public final class ImageGenerationClient {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public interface Callback {
        void onSuccess(byte[] pngOrJpegBytes, String mimeType, String revisedPrompt);

        void onError(String message);
    }

    private ImageGenerationClient() {
    }

    /**
     * @param baseUrl provider API root, e.g. https://api.openai.com/v1
     * @param model   e.g. dall-e-3, gpt-image-1, or gateway-specific id
     * @param size    1024x1024, 1024x1792, 1792x1024 when supported
     */
    public static void generate(
            String baseUrl,
            String apiKey,
            String model,
            String prompt,
            String size,
            String quality,
            Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new IllegalArgumentException("Add an API key that supports image generation first.");
                }
                if (prompt == null || prompt.trim().isEmpty()) {
                    throw new IllegalArgumentException("Enter an image prompt.");
                }
                if (prompt.length() > 4000) {
                    throw new IllegalArgumentException("Prompt is limited to 4000 characters.");
                }
                String base = normalizeBase(baseUrl);
                String modelId = (model == null || model.trim().isEmpty()) ? "dall-e-3" : model.trim();
                String imageSize = (size == null || size.trim().isEmpty()) ? "1024x1024" : size.trim();
                String q = (quality == null || quality.trim().isEmpty()) ? "standard" : quality.trim();

                JSONObject body = new JSONObject();
                body.put("model", modelId);
                body.put("prompt", prompt.trim());
                body.put("n", 1);
                body.put("size", imageSize);
                // b64 is portable on mobile; URL requires a second download
                body.put("response_format", "b64_json");
                if ("dall-e-3".equals(modelId) || modelId.contains("dall-e-3")) {
                    body.put("quality", q);
                }

                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(base + "/images/generations").openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(20_000);
                    connection.setReadTimeout(180_000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                    connection.setRequestProperty("User-Agent", "Kairo-Android/0.9");
                    byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                    connection.setFixedLengthStreamingMode(payload.length);
                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(payload);
                    }
                    int status = connection.getResponseCode();
                    InputStream stream = status >= 200 && status < 300
                            ? connection.getInputStream() : connection.getErrorStream();
                    String response = read(stream);
                    if (status < 200 || status >= 300) {
                        String detail = response;
                        try {
                            JSONObject err = new JSONObject(response);
                            if (err.has("error")) {
                                Object e = err.get("error");
                                if (e instanceof JSONObject) {
                                    detail = ((JSONObject) e).optString("message", response);
                                } else {
                                    detail = String.valueOf(e);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                        throw new IllegalStateException("Image API HTTP " + status + ": " + detail);
                    }
                    JSONObject root = new JSONObject(response);
                    JSONArray data = root.optJSONArray("data");
                    if (data == null || data.length() == 0) {
                        throw new IllegalStateException("Provider returned no image data.");
                    }
                    JSONObject first = data.getJSONObject(0);
                    String b64 = first.optString("b64_json", "");
                    String revised = first.optString("revised_prompt", prompt.trim());
                    if (b64.isEmpty()) {
                        // Some gateways only return URL
                        String url = first.optString("url", "");
                        if (url.isEmpty()) {
                            throw new IllegalStateException("No b64_json or url in response.");
                        }
                        byte[] downloaded = download(url);
                        callback.onSuccess(downloaded, guessMime(downloaded), revised);
                        return;
                    }
                    byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                    callback.onSuccess(bytes, guessMime(bytes), revised);
                } finally {
                    if (connection != null) connection.disconnect();
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                callback.onError(msg == null || msg.trim().isEmpty() ? "Image generation failed." : msg);
            }
        });
    }

    private static byte[] download(String imageUrl) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(imageUrl).openConnection();
            c.setConnectTimeout(20_000);
            c.setReadTimeout(60_000);
            c.setRequestProperty("User-Agent", "Kairo-Android/0.9");
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Image download HTTP " + status);
            }
            InputStream in = c.getInputStream();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            int total = 0;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > 8_000_000) throw new IllegalStateException("Image too large.");
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String guessMime(byte[] bytes) {
        if (bytes != null && bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        return "image/png";
    }

    private static String normalizeBase(String baseUrl) {
        String base = baseUrl == null || baseUrl.trim().isEmpty()
                ? "https://api.openai.com/v1" : baseUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.endsWith("/v1")) return base;
        // OpenRouter / custom roots
        if (base.contains("openrouter.ai")) {
            if (!base.endsWith("/api/v1")) {
                if (base.endsWith("/api")) return base + "/v1";
            }
        }
        return base;
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
