package com.kairo.app.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.kairo.app.data.ChatMessage;
import com.kairo.app.data.ConversationSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Persists a bounded local history so Chat feels like a real workspace after relaunch. */
public final class ConversationStore {
    private static final String PREFS = "kairo_conversations";
    private static final String KEY_SESSIONS = "sessions";
    private static final int MAX_SESSIONS = 40;
    private static final int MAX_STORED_MESSAGES_PER_SESSION = 50;
    private static final int MAX_STORED_CONTENT_CHARS = 12_000;
    private final SharedPreferences preferences;

    public ConversationStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized List<ConversationSession> load() {
        List<ConversationSession> sessions = new ArrayList<>();
        String raw = preferences.getString(KEY_SESSIONS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.optJSONObject(index);
                if (item == null) continue;
                JSONArray messageArray = item.optJSONArray("messages");
                List<ChatMessage> messages = new ArrayList<>();
                if (messageArray != null) {
                    for (int messageIndex = 0; messageIndex < messageArray.length(); messageIndex++) {
                        JSONObject message = messageArray.optJSONObject(messageIndex);
                        if (message == null) continue;
                        String role = message.optString("role", "user");
                        String content = message.optString("content", "");
                        if (!content.isEmpty()) messages.add(new ChatMessage(role, content));
                    }
                }
                sessions.add(new ConversationSession(
                        item.optString("id", String.valueOf(System.nanoTime())),
                        item.optString("title", "New conversation"),
                        item.optLong("updatedAt", 0L),
                        messages));
            }
        } catch (Exception ignored) {
            // A corrupt local history should never prevent the app from starting.
        }
        Collections.sort(sessions, (left, right) -> Long.compare(right.getUpdatedAt(), left.getUpdatedAt()));
        return sessions;
    }

    public synchronized void save(List<ConversationSession> sessions) {
        JSONArray array = new JSONArray();
        if (sessions != null) {
            List<ConversationSession> copy = new ArrayList<>(sessions);
            Collections.sort(copy, (left, right) -> Long.compare(right.getUpdatedAt(), left.getUpdatedAt()));
            int count = 0;
            for (ConversationSession session : copy) {
                if (count++ >= MAX_SESSIONS) break;
                try {
                    JSONObject item = new JSONObject();
                    item.put("id", session.getId());
                    item.put("title", session.getTitle());
                    item.put("updatedAt", session.getUpdatedAt());
                    JSONArray messages = new JSONArray();
                    List<ChatMessage> source = session.getMessages();
                    int firstMessage = Math.max(0, source.size() - MAX_STORED_MESSAGES_PER_SESSION);
                    for (int messageIndex = firstMessage; messageIndex < source.size(); messageIndex++) {
                        ChatMessage message = source.get(messageIndex);
                        JSONObject value = new JSONObject();
                        value.put("role", message.getRole());
                        String content = message.getContent() == null ? "" : message.getContent();
                        // Do not retain recognizable provider credentials in local history even if
                        // a message came from an older app version before the composer guard.
                        content = ApiKeyDetector.redact(content);
                        if (content.length() > MAX_STORED_CONTENT_CHARS) {
                            content = content.substring(0, MAX_STORED_CONTENT_CHARS) + "…";
                        }
                        value.put("content", content);
                        messages.put(value);
                    }
                    item.put("messages", messages);
                    array.put(item);
                } catch (Exception ignored) {
                    // Skip only the malformed item, not the rest of history.
                }
            }
        }
        preferences.edit().putString(KEY_SESSIONS, array.toString()).apply();
    }

    public static String newId() {
        return String.valueOf(System.currentTimeMillis()) + "_" + String.valueOf(System.nanoTime());
    }
}
