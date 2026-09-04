package com.kairo.app.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.kairo.app.data.Artifact;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Private, bounded artifact workspace. Files are kept under Context.getFilesDir(), never in
 * shared storage, and names are sanitized to prevent path traversal.
 */
public final class ArtifactStore implements com.kairo.app.agent.ArtifactAgent {
    private static final String PREFS = "kairo_artifacts";
    private static final String KEY_INDEX = "index";
    private static final int MAX_ARTIFACTS = 100;
    private static final int MAX_CONTENT_CHARS = 500_000;

    private final File directory;
    private final SharedPreferences preferences;

    public ArtifactStore(Context context) {
        Context app = context.getApplicationContext();
        directory = new File(app.getFilesDir(), "artifacts");
        if (!directory.exists()) directory.mkdirs();
        preferences = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public synchronized Artifact create(String name, String language, String content) throws Exception {
        String safeName = safeName(name);
        String value = content == null ? "" : content;
        if (value.length() > MAX_CONTENT_CHARS) {
            throw new IllegalArgumentException("Artifacts are limited to 500,000 characters.");
        }
        String id = UUID.randomUUID().toString();
        Artifact artifact = new Artifact(id, safeName,
                language == null || language.trim().isEmpty() ? Artifact.inferLanguage(safeName) : language,
                System.currentTimeMillis(), value);
        write(artifact);
        List<Artifact> all = list();
        all.add(artifact);
        saveIndex(all);
        return artifact;
    }

    public synchronized Artifact update(Artifact old, String name, String language, String content) throws Exception {
        if (old == null) throw new IllegalArgumentException("Artifact not found.");
        String value = content == null ? "" : content;
        if (value.length() > MAX_CONTENT_CHARS) throw new IllegalArgumentException("Artifact is too large.");
        Artifact updated = new Artifact(old.getId(), safeName(name),
                language == null || language.trim().isEmpty() ? Artifact.inferLanguage(name) : language,
                System.currentTimeMillis(), value);
        write(updated);
        List<Artifact> all = list();
        for (int index = 0; index < all.size(); index++) {
            if (all.get(index).getId().equals(old.getId())) {
                all.set(index, updated);
                break;
            }
        }
        saveIndex(all);
        return updated;
    }

    public synchronized List<Artifact> list() {
        List<Artifact> result = new ArrayList<>();
        String raw = preferences.getString(KEY_INDEX, "[]");
        try {
            JSONArray index = new JSONArray(raw);
            for (int i = 0; i < index.length(); i++) {
                JSONObject item = index.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                if (id.isEmpty()) continue;
                File file = new File(directory, id + ".artifact");
                if (!file.exists()) continue;
                result.add(new Artifact(id, item.optString("name", "untitled.txt"),
                        item.optString("language", "text"), item.optLong("updatedAt", 0L), read(file)));
            }
        } catch (Exception ignored) {
            // A damaged index should not prevent the chat app from launching.
        }
        Collections.sort(result, (left, right) -> Long.compare(right.getUpdatedAt(), left.getUpdatedAt()));
        return result;
    }

    public synchronized void delete(Artifact artifact) {
        if (artifact == null) return;
        File file = new File(directory, artifact.getId() + ".artifact");
        if (file.exists()) file.delete();
        List<Artifact> remaining = list();
        remaining.removeIf(item -> item.getId().equals(artifact.getId()));
        saveIndex(remaining);
    }

    private void write(Artifact artifact) throws Exception {
        File file = new File(directory, artifact.getId() + ".artifact");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(artifact.getContent().getBytes(StandardCharsets.UTF_8));
        }
    }

    private String read(File file) {
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(file.length(), MAX_CONTENT_CHARS * 4L)];
            int count = input.read(buffer);
            return count <= 0 ? "" : new String(buffer, 0, count, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void saveIndex(List<Artifact> artifacts) {
        JSONArray index = new JSONArray();
        int count = 0;
        for (Artifact artifact : artifacts) {
            if (count++ >= MAX_ARTIFACTS) break;
            try {
                JSONObject item = new JSONObject();
                item.put("id", artifact.getId());
                item.put("name", artifact.getName());
                item.put("language", artifact.getLanguage());
                item.put("updatedAt", artifact.getUpdatedAt());
                index.put(item);
            } catch (Exception ignored) {
            }
        }
        preferences.edit().putString(KEY_INDEX, index.toString()).apply();
    }

    private String safeName(String name) {
        String value = name == null ? "untitled.txt" : name.trim();
        value = value.replace('\\', '/');
        int slash = value.lastIndexOf('/');
        if (slash >= 0) value = value.substring(slash + 1);
        value = value.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (value.isEmpty() || ".".equals(value) || "..".equals(value)) value = "untitled.txt";
        if (value.length() > 96) value = value.substring(0, 96);
        return value;
    }
}
